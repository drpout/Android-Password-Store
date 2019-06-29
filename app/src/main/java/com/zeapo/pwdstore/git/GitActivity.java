package com.zeapo.pwdstore.git;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.zeapo.pwdstore.db.entity.StoreEntity;
import com.zeapo.pwdstore.db.entity.AuthMethod;
import com.zeapo.pwdstore.db.PasswordStoreDb;
import com.zeapo.pwdstore.R;
import com.zeapo.pwdstore.UserPreference;
import com.zeapo.pwdstore.git.config.SshApiSessionFactory;
import com.zeapo.pwdstore.utils.PasswordRepository;
import com.zeapo.pwdstore.utils.SplitFocusAwareTextWatcher;
import com.zeapo.pwdstore.utils.UpdateFocusAwareTextWatcher;

import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jgit.lib.UserConfig;
import org.eclipse.jgit.transport.RemoteConfig;
import org.eclipse.jgit.transport.URIish;

import java.io.File;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.net.URISyntaxException;

public class GitActivity extends AppCompatActivity {
    public static final int REQUEST_PULL = 101;
    public static final int REQUEST_PUSH = 102;
    public static final int REQUEST_CLONE = 103;
    public static final int REQUEST_INIT = 104;
    public static final int EDIT_SERVER = 105;
    public static final int REQUEST_SYNC = 106;
    public static final int REQUEST_CREATE = 107;
    public static final int EDIT_GIT_CONFIG = 108;
    public static final int BREAK_OUT_OF_DETACHED = 109;
    private static final String TAG = "GitAct";
    private static final String emailPattern = "^[^@]+@[^@]+$";
    private Activity activity;
    private Context context;
    private String protocol;
    private AuthMethod authMethod;
    private String hostname;
    private SharedPreferences settings;
    private PasswordStoreDb db;
    private StoreEntity currentStore;
    private SshApiSessionFactory.IdentityBuilder identityBuilder;
    private SshApiSessionFactory.ApiIdentity identity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        context = getApplicationContext();
        activity = this;

        settings = PreferenceManager.getDefaultSharedPreferences(this.context);
        db = PasswordStoreDb.Companion.get(this.getApplicationContext());
        currentStore = db.storeDao().getByName("default");

        // init the server information
        final EditText serverUrl = findViewById(R.id.server_url);
        final EditText serverPort = findViewById(R.id.server_port);
        final EditText serverPath = findViewById(R.id.server_path);
        final EditText serverUser = findViewById(R.id.server_user);
        final EditText serverUri = findViewById(R.id.clone_uri);
        Repository repository = PasswordRepository.getRepository(PasswordRepository.getRepositoryDirectory(activity.getApplicationContext(), currentStore));
        if(repository != null) {
            try {
                StoredConfig repoConfig = repository.getConfig();
                RemoteConfig remoteConfig = new RemoteConfig(repoConfig, "origin");
                URIish uriish = remoteConfig.getURIs().get(0);
                protocol = uriish.getScheme();
                serverUrl.setText(uriish.getHost());
                serverPort.setText(uriish.getPort());
                serverUser.setText(uriish.getUser());
                serverPath.setText(uriish.getPath());
            } catch(URISyntaxException e) {
                protocol = "ssh://";
                Log.e(TAG, "Caught exception while creating RemoteConfig " + e);
            }
        } else {
            protocol = "ssh://";
            Log.d(TAG, "onCreate: git repository is not initialized.");
        }

        authMethod = currentStore.getGitRemote().getAuth();

        int operationCode = getIntent().getExtras().getInt("Operation");

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        switch (operationCode) {
            case REQUEST_CLONE:
            case EDIT_SERVER:
                setContentView(R.layout.activity_git_clone);
                setTitle(R.string.title_activity_git_clone);

                // init the spinner for authentication methods
                final Spinner authMethodSpinner = findViewById(R.id.auth_method);
                final ArrayAdapter<CharSequence> authMethodAdapter = ArrayAdapter.createFromResource(this,
                        R.array.auth_methods, android.R.layout.simple_spinner_item);
                authMethodAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                authMethodSpinner.setAdapter(authMethodAdapter);
                authMethodSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id) {
                        authMethod = AuthMethod.values()[position];
                        currentStore.getGitRemote().setAuth(authMethod);
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> adapterView) {

                    }
                });

                // init the spinner for protocols
                final Spinner protocolSpinner = findViewById(R.id.clone_protocol);
                ArrayAdapter<CharSequence> protocolAdapter = ArrayAdapter.createFromResource(this,
                        R.array.clone_protocols, android.R.layout.simple_spinner_item);
                protocolAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                protocolSpinner.setAdapter(protocolAdapter);
                protocolSpinner.setOnItemSelectedListener(
                        new AdapterView.OnItemSelectedListener() {
                            @Override
                            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                                protocol = ((Spinner) findViewById(R.id.clone_protocol)).getSelectedItem().toString();
                                if (protocol.equals("ssh://")) {
                                    ((EditText) findViewById(R.id.clone_uri)).setHint("user@hostname:path");

                                    ((EditText) findViewById(R.id.server_port)).setHint(R.string.default_ssh_port);

                                    // select ssh-key auth mode as default and enable the spinner in case it was disabled
                                    authMethodSpinner.setSelection(0);
                                    authMethodSpinner.setEnabled(true);

                                    // however, if we have some saved that, that's more important!
                                    authMethodSpinner.setSelection(authMethod.ordinal());
                                } else {
                                    ((EditText) findViewById(R.id.clone_uri)).setHint("hostname/path");

                                    ((EditText) findViewById(R.id.server_port)).setHint(R.string.default_https_port);

                                    // select user/pwd auth-mode and disable the spinner
                                    authMethodSpinner.setSelection(1);
                                    authMethodSpinner.setEnabled(false);
                                }

                                updateURI();
                            }

                            @Override
                            public void onNothingSelected(AdapterView<?> adapterView) {

                            }
                        }
                );

                if (protocol.equals("ssh://")) {
                    protocolSpinner.setSelection(0);
                } else {
                    protocolSpinner.setSelection(1);
                }

                serverUrl.addTextChangedListener(new UpdateFocusAwareTextWatcher(serverUrl, this));
                serverPort.addTextChangedListener(new UpdateFocusAwareTextWatcher(serverPort, this));
                serverUser.addTextChangedListener(new UpdateFocusAwareTextWatcher(serverUser, this));
                serverPath.addTextChangedListener(new UpdateFocusAwareTextWatcher(serverPath, this));
                serverUri.addTextChangedListener(new SplitFocusAwareTextWatcher(serverUri, this));

                if (operationCode == EDIT_SERVER) {
                    findViewById(R.id.clone_button).setVisibility(View.INVISIBLE);
                    findViewById(R.id.save_button).setVisibility(View.VISIBLE);
                } else {
                    findViewById(R.id.clone_button).setVisibility(View.VISIBLE);
                    findViewById(R.id.save_button).setVisibility(View.INVISIBLE);
                }

                updateURI();

                break;
            case EDIT_GIT_CONFIG:
                setContentView(R.layout.activity_git_config);
                setTitle(R.string.title_activity_git_config);

                showGitConfig();
                break;
            case REQUEST_PULL:
                syncRepository(REQUEST_PULL);
                break;

            case REQUEST_PUSH:
                syncRepository(REQUEST_PUSH);
                break;

            case REQUEST_SYNC:
                syncRepository(REQUEST_SYNC);
                break;
        }


    }

    /**
     * Fills in the server_uri field with the information coming from other fields
     */
    public void updateURI() {
        EditText uri = findViewById(R.id.clone_uri);
        EditText serverUrl = findViewById(R.id.server_url);
        EditText serverPort = findViewById(R.id.server_port);
        EditText serverPath = findViewById(R.id.server_path);
        EditText serverUser = findViewById(R.id.server_user);

        if (uri != null) {
            switch (protocol) {
                case "ssh://": {
                    String hostname =
                            serverUser.getText()
                                    + "@" +
                                    serverUrl.getText().toString().trim()
                                    + ":";
                    if (serverPort.getText().toString().equals("22")) {
                        hostname += serverPath.getText().toString();

                        findViewById(R.id.warn_url).setVisibility(View.GONE);
                    } else {
                        TextView warn_url = findViewById(R.id.warn_url);
                        if (!serverPath.getText().toString().matches("/.*") && !serverPort.getText().toString().isEmpty()) {
                            warn_url.setText(R.string.warn_malformed_url_port);
                            warn_url.setVisibility(View.VISIBLE);
                        } else {
                            warn_url.setVisibility(View.GONE);
                        }
                        hostname += serverPort.getText().toString() + serverPath.getText().toString();
                    }

                    if (!hostname.equals("@:")) uri.setText(hostname);
                }
                break;
                case "https://": {
                    StringBuilder hostname = new StringBuilder();
                    hostname.append(serverUrl.getText().toString().trim());

                    if (serverPort.getText().toString().equals("443")) {
                        hostname.append(serverPath.getText().toString());

                        findViewById(R.id.warn_url).setVisibility(View.GONE);
                    } else {
                        hostname.append("/");
                        hostname.append(serverPort.getText().toString())
                                .append(serverPath.getText().toString());
                    }

                    if (!hostname.toString().equals("@/")) uri.setText(hostname);
                }
                break;
                default:
                    break;
            }

        }
    }

    /**
     * Splits the information in server_uri into the other fields
     */
    public void splitURI() {
        EditText serverUri = findViewById(R.id.clone_uri);
        EditText serverUrl = findViewById(R.id.server_url);
        EditText serverPort = findViewById(R.id.server_port);
        EditText serverPath = findViewById(R.id.server_path);
        EditText serverUser = findViewById(R.id.server_user);

        String uri = serverUri.getText().toString();
        Pattern pattern = Pattern.compile("(.+)@([\\w\\d.]+):([\\d]+)*(.*)");
        Matcher matcher = pattern.matcher(uri);
        if (matcher.find()) {
            int count = matcher.groupCount();
            if (count > 1) {
                serverUser.setText(matcher.group(1));
                serverUrl.setText(matcher.group(2));
            }
            if (count == 4) {
                serverPort.setText(matcher.group(3));
                serverPath.setText(matcher.group(4));

                TextView warn_url = findViewById(R.id.warn_url);
                if (!serverPath.getText().toString().matches("/.*") && !serverPort.getText().toString().isEmpty()) {
                    warn_url.setText(R.string.warn_malformed_url_port);
                    warn_url.setVisibility(View.VISIBLE);
                } else {
                    warn_url.setVisibility(View.GONE);
                }
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        updateURI();
    }

    @Override
    protected void onDestroy() {
        // Do not leak the service connection
        if (identityBuilder != null) {
            identityBuilder.close();
            identityBuilder = null;
        }
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.git_clone, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.user_pref) {
            try {
                Intent intent = new Intent(this, UserPreference.class);
                startActivity(intent);
            } catch (Exception e) {
                System.out.println("Exception caught :(");
                e.printStackTrace();
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Saves the configuration found in the form
     */
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private boolean saveConfiguration() {
        // remember the settings
        SharedPreferences.Editor editor = settings.edit();

        editor.putString("git_remote_server", ((EditText) findViewById(R.id.server_url)).getText().toString());
        editor.putString("git_remote_location", ((EditText) findViewById(R.id.server_path)).getText().toString());
        editor.putString("git_remote_username", ((EditText) findViewById(R.id.server_user)).getText().toString());
        editor.putString("git_remote_protocol", protocol);
        editor.putString("git_remote_auth", connectionMode);
        editor.putString("git_remote_port", ((EditText) findViewById(R.id.server_port)).getText().toString());
        editor.putString("git_remote_uri", ((EditText) findViewById(R.id.clone_uri)).getText().toString());

        // 'save' hostname variable for use by addRemote() either here or later
        // in syncRepository()
        hostname = ((EditText) findViewById(R.id.clone_uri)).getText().toString();
        String port = ((EditText) findViewById(R.id.server_port)).getText().toString();
        // don't ask the user, take off the protocol that he puts in
        hostname = hostname.replaceFirst("^.+://", "");
        ((TextView) findViewById(R.id.clone_uri)).setText(hostname);

        if (!protocol.equals("ssh://")) {
            hostname = protocol + hostname;
        } else {
            // if the port is explicitly given, jgit requires the ssh://
            if (!port.isEmpty() && !port.equals("22"))
                hostname = protocol + hostname;

            // did he forget the username?
            if (!hostname.matches("^.+@.+")) {
                new AlertDialog.Builder(this).
                        setMessage(activity.getResources().getString(R.string.forget_username_dialog_text)).
                        setPositiveButton(activity.getResources().getString(R.string.dialog_oops), null).
                        show();
                return false;
            }
        }
        if (PasswordRepository.isInitialized() && settings.getBoolean("repository_initialized", false)) {
            // don't just use the clone_uri text, need to use hostname which has
            // had the proper protocol prepended
            PasswordRepository.addRemote("origin", hostname, true);
        }

        editor.apply();
        return true;
    }

    /**
     * Save the repository information to the shared preferences settings
     */
    public void saveConfiguration(View view) {
        if (!saveConfiguration())
            return;
        finish();
    }

    private void showGitConfig() {
        // init the server information
        final EditText git_user_name = findViewById(R.id.git_user_name);
        final EditText git_user_email = findViewById(R.id.git_user_email);
        final Button abort = findViewById(R.id.git_abort_rebase);

        git_user_name.setText(settings.getString("git_config_user_name", ""));
        git_user_email.setText(settings.getString("git_config_user_email", ""));

        // git status
        Repository repo = PasswordRepository.getRepository(PasswordRepository.getRepositoryDirectory(activity.getApplicationContext(), currentStore));
        if (repo != null) {
            final TextView git_commit_hash = findViewById(R.id.git_commit_hash);
            try {
                ObjectId objectId = repo.resolve(Constants.HEAD);
                Ref ref = repo.getRef("refs/heads/master");
                String head = ref.getObjectId().equals(objectId) ? ref.getName() : "DETACHED";
                git_commit_hash.setText(String.format("%s (%s)", objectId.abbreviate(8).name(), head));

                // enable the abort button only if we're rebasing
                abort.setEnabled(repo.getRepositoryState().isRebasing());
            } catch (Exception e) {
                // ignore
            }
        }
    }

    private boolean saveGitConfigs() {
        // remember the settings
        SharedPreferences.Editor editor = settings.edit();

        String email = ((EditText) findViewById(R.id.git_user_email)).getText().toString();
        editor.putString("git_config_user_email", email);
        editor.putString("git_config_user_name", ((EditText) findViewById(R.id.git_user_name)).getText().toString());

        if (!email.matches(emailPattern)) {
            new AlertDialog.Builder(this).
                    setMessage(activity.getResources().getString(R.string.invalid_email_dialog_text)).
                    setPositiveButton(activity.getResources().getString(R.string.dialog_oops), null).
                    show();
            return false;
        }

        editor.apply();
        return true;
    }

    public void applyGitConfigs(View view) {
        if (!saveGitConfigs())
            return;

        String git_user_name = settings.getString("git_config_user_name", "");
        String git_user_email = settings.getString("git_config_user_email", "");

        PasswordRepository.setUserName(git_user_name);
        PasswordRepository.setUserEmail(git_user_email);

        finish();
    }

    public void abortRebase(View view) {
        launchGitOperation(BREAK_OUT_OF_DETACHED);
    }

    /**
     * Clones the repository, the directory exists, deletes it
     */
    public void cloneRepository(View view) {
        if (PasswordRepository.getRepository(null) == null) {
            PasswordRepository.initialize(this, currentStore);
        }
        File localDir = PasswordRepository.getRepositoryDirectory(context, currentStore);

        if (!saveConfiguration())
            return;

        // Warn if non-empty folder unless it's a just-initialized store that has just a .git folder
        if (localDir.exists() && localDir.listFiles().length != 0
                && !(localDir.listFiles().length == 1 && localDir.listFiles()[0].getName().equals(".git"))) {
            new AlertDialog.Builder(this).
                    setTitle(R.string.dialog_delete_title).
                    setMessage(getResources().getString(R.string.dialog_delete_msg) + " " + localDir.toString()).
                    setCancelable(false).
                    setPositiveButton(R.string.dialog_delete,
                            (dialog, id) -> {
                                try {
                                    FileUtils.deleteDirectory(localDir);
                                    launchGitOperation(REQUEST_CLONE);
                                } catch (IOException e) {
                                    //TODO Handle the exception correctly if we are unable to delete the directory...
                                    e.printStackTrace();
                                    new AlertDialog.Builder(GitActivity.this).setMessage(e.getMessage()).show();
                                }

                                dialog.cancel();
                            }
                    ).
                    setNegativeButton(R.string.dialog_do_not_delete,
                            (dialog, id) -> dialog.cancel()
                    ).
                    show();
        } else {
            try {
                // Silently delete & replace the lone .git folder if it exists
                if (localDir.exists() && localDir.listFiles().length == 1 && localDir.listFiles()[0].getName().equals(".git")) {
                    try {
                        FileUtils.deleteDirectory(localDir);
                    } catch (IOException e) {
                        e.printStackTrace();
                        new AlertDialog.Builder(GitActivity.this).setMessage(e.getMessage()).show();
                    }
                }
            } catch (Exception e) {
                //This is what happens when jgit fails :(
                //TODO Handle the diffent cases of exceptions
                e.printStackTrace();
                new AlertDialog.Builder(this).setMessage(e.getMessage()).show();
            }
            launchGitOperation(REQUEST_CLONE);
        }
    }

    /**
     * Syncs the local repository with the remote one (either pull or push)
     *
     * @param operation the operation to execute can be REQUEST_PULL or REQUEST_PUSH
     */
    private void syncRepository(int operation) {
        if (settings.getString("git_remote_username", "").isEmpty() ||
                settings.getString("git_remote_server", "").isEmpty() ||
                settings.getString("git_remote_location", "").isEmpty())
            new AlertDialog.Builder(this)
                    .setMessage(activity.getResources().getString(R.string.set_information_dialog_text))
                    .setPositiveButton(activity.getResources().getString(R.string.dialog_positive), (dialogInterface, i) -> {
                        Intent intent = new Intent(activity, UserPreference.class);
                        startActivityForResult(intent, REQUEST_PULL);
                    })
                    .setNegativeButton(activity.getResources().getString(R.string.dialog_negative), (dialogInterface, i) -> {
                        // do nothing :(
                        setResult(RESULT_OK);
                        finish();
                    })
                    .show();

        else {
            // check that the remote origin is here, else add it
            PasswordRepository.addRemote("origin", hostname, false);
            launchGitOperation(operation);
        }
    }

    /**
     * Attempt to launch the requested GIT operation. Depending on the configured auth, it may not
     * be possible to launch the operation immediately. In that case, this function may launch an
     * intermediate activity instead, which will gather necessary information and post it back via
     * onActivityResult, which will then re-call this function. This may happen multiple times,
     * until either an error is encountered or the operation is successfully launched.
     *
     * @param operation The type of GIT operation to launch
     */
    protected void launchGitOperation(int operation) {
        GitOperation op;
        File localDir = PasswordRepository.getRepositoryDirectory(context, currentStore);

        try {

            // Before launching the operation with OpenKeychain auth, we need to issue several requests
            // to the OpenKeychain API. IdentityBuild will take care of launching the relevant intents,
            // we just need to keep calling it until it returns a completed ApiIdentity.
            if (authMethod.equals(AuthMethod.PGP) && identity == null) {
                // Lazy initialization of the IdentityBuilder
                if (identityBuilder == null) {
                    identityBuilder = new SshApiSessionFactory.IdentityBuilder(this);
                }

                // Try to get an ApiIdentity and bail if one is not ready yet. The builder will ensure
                // that onActivityResult is called with operation again, which will re-invoke us here
                identity = identityBuilder.tryBuild(operation);
                if (identity == null)
                    return;
            }

            switch (operation) {
                case REQUEST_CLONE:
                case GitOperation.GET_SSH_KEY_FROM_CLONE:
                    op = new CloneOperation(localDir, activity).setCommand(hostname);
                    break;

                case REQUEST_PULL:
                    op = new PullOperation(localDir, activity).setCommand();
                    break;

                case REQUEST_PUSH:
                    op = new PushOperation(localDir, activity).setCommand();
                    break;

                case REQUEST_SYNC:
                    op = new SyncOperation(localDir, activity).setCommands();
                    break;

                case BREAK_OUT_OF_DETACHED:
                    op = new BreakOutOfDetached(localDir, activity).setCommands();
                    break;

                case SshApiSessionFactory.POST_SIGNATURE:
                    return;

                default:
                    Log.e(TAG, "Operation not recognized : " + operation);
                    setResult(RESULT_CANCELED);
                    finish();
                    return;
            }

            op.executeAfterAuthentication(authMethod,
                    settings.getString("git_remote_username", "git"),
                    new File(getFilesDir() + "/.ssh_key"),
                    identity);
        } catch (Exception e) {
            e.printStackTrace();
            new AlertDialog.Builder(this).setMessage(e.getMessage()).show();
        }
    }

    protected void onActivityResult(int requestCode, int resultCode,
                                    Intent data) {

        // In addition to the pre-operation-launch series of intents for OpenKeychain auth
        // that will pass through here and back to launchGitOperation, there is one
        // synchronous operation that happens /after/ the operation has been launched in the
        // background thread - the actual signing of the SSH challenge. We pass through the
        // completed signature to the ApiIdentity, which will be blocked in the other thread
        // waiting for it.
        if (requestCode == SshApiSessionFactory.POST_SIGNATURE && identity != null)
            identity.postSignature(data);

        if (resultCode == RESULT_CANCELED) {
            setResult(RESULT_CANCELED);
            finish();
        } else if (resultCode == RESULT_OK) {
            // If an operation has been re-queued via this mechanism, let the
            // IdentityBuilder attempt to extract some updated state from the intent before
            // trying to re-launch the operation.
            if (identityBuilder != null) {
                identityBuilder.consume(data);
            }
            launchGitOperation(requestCode);
        }
    }

}
