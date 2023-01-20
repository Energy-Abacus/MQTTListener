package org.abacus.script;

import org.keycloak.authorization.client.AuthzClient;
import org.keycloak.representations.idm.authorization.AuthorizationRequest;
import org.keycloak.representations.idm.authorization.AuthorizationResponse;

public class KeycloakToken {

    String kUsername = System.getenv("keycloak_user");
    String kPassword = System.getenv("keycloak_passwd");
    AuthzClient authzClient = AuthzClient.create();
    AuthorizationRequest request = new AuthorizationRequest();
    AuthorizationResponse response = authzClient.authorization(kUsername, kPassword).authorize(request);
    String rpt = response.getToken();

    public String getRpt() {
        return rpt;
    }

}
