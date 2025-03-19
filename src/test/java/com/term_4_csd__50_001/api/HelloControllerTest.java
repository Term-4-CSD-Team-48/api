package com.term_4_csd__50_001.api;

import org.junit.jupiter.api.Test;

public class HelloControllerTest extends BaseTest {

    // @Test
    public void indexTest() throws Exception {
        webTestClient.get().uri("/").exchange().expectStatus().isUnauthorized();
    }

}
