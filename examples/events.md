# Listening to GPGS Events

The snippet below shows how to initialise the plugin manually **after** `deviceready`, listen for the `gpgs.signin`, `gpgs.signout`, and `gpgs.availability` events, and enable or disable game-related UI accordingly.

```javascript
/**
 * Example: Initialise the Google-Play-Games plugin and react to sign-in events
 */

document.addEventListener('deviceready', onDeviceReady, false);

function onDeviceReady () {
    /* ----- 1. Register listeners BEFORE calling initialise() ----- */

    // Sign-in event
    function onSignIn (event) {
        const data = event.detail;
        if (data.isSignedIn) {
            console.log('[GPGS] Signed in ✅');
            enableGameFeatures();
        } else {
            console.warn('[GPGS] Sign-in FAILED ❌', data.error);
            showManualSignInButton();
        }
    }
    document.addEventListener('gpgs.signin', onSignIn);

    // Sign-out event
    document.addEventListener('gpgs.signout', (e) => {
        console.log('[GPGS] Signed out:', e.detail.reason);
        disableGameFeatures();
    });

    // Availability event
    document.addEventListener('gpgs.availability', (e) => {
        const info = e.detail;
        if (info.available) {
            console.log('[GPGS] Play-Services available ✅');
        } else {
            console.warn('[GPGS] Play-Services unavailable ❌', info.errorString);
            if (info.isUserResolvable) {
                // Prompt user to resolve (update / enable services)
            }
        }
    });

    /* ----- 2. Trigger the silent sign-in flow ----- */

    GPGS.initialize()
        .then(() => console.log('[GPGS] initialise() dispatched'))
        .catch(console.error);

    /* ----- 3. Optional cleanup later ----- */
    // document.removeEventListener('gpgs.signin', onSignIn);
}

/* Helper stubs – replace with real game logic */
function enableGameFeatures () {
    GPGS.getPlayerScore('<leaderboard_id>')
        .then((score) => console.log('Current score:', score))
        .catch(console.error);
}
function disableGameFeatures () {}
function showManualSignInButton () {}
```

> ℹ️ The plugin **does not** attempt silent sign-in automatically. You decide when `GPGS.initialize()` is called and, consequently, when the events fire. 