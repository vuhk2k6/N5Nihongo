package com.vunv.n5nihongo.data.auth

import com.facebook.CallbackManager

/** Shared between [com.vunv.n5nihongo.MainActivity] and login UI for Facebook SDK activity results. */
object FacebookAuthCallbacks {
    var callbackManager: CallbackManager? = null
}
