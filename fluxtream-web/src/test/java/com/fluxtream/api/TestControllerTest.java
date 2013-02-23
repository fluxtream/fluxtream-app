package com.fluxtream.api;

import com.fluxtream.utils.HttpUtils;
import org.junit.Test;

/**
 *
 * @author Candide Kemmler (candide@fluxtream.com)
 */
public class TestControllerTest {

    @Test
    public void testSetAttribute() throws Exception {
        String username = "yourusername";
        for(int i=0; i<10; i++)
            HttpUtils.fetch("http://localhost:8082/api/test/setAttribute?att=çamarcheenfin", username, "yourpassword");
    }

}
