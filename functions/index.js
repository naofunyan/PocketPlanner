const functions = require('firebase-functions');
const { GoogleAuth } = require('google-auth-library');
const path = require('path');

exports.getVertexAccessToken = functions.https.onCall(async (data, context) => {
    // SECURITY: Ensure the request comes from a user logged into your app
    // Uncomment this once your app has a login screen!
    // if (!context.auth) {
    //     throw new functions.https.HttpsError(
    //         'unauthenticated',
    //         'The function must be called while authenticated.'
    //     );
    // }

    try {
        // Read the service account file that is now safely stored on the server
        const auth = new GoogleAuth({
            keyFile: path.join(__dirname, 'service_account.json'),
            scopes: ['https://www.googleapis.com/auth/cloud-platform']
        });

        const client = await auth.getClient();
        const accessToken = await client.getAccessToken();

        // Return the temporary token to the Android app
        return {
            token: accessToken.token
        };
    } catch (error) {
        console.error("Error generating token:", error);
        throw new functions.https.HttpsError('internal', 'Unable to generate access token');
    }
});