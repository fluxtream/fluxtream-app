package org.fluxtream.connectors;

import java.io.File;
import org.fluxtream.domain.ApiKey;

/**
 * User: candide
 * Date: 12/08/13
 * Time: 09:55
 */
public interface FileUploadSupport {

    public int importFile(ApiKey apiKey, File file) throws Exception;

}
