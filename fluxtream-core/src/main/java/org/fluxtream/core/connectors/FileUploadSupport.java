package org.fluxtream.core.connectors;

import java.io.File;
import org.fluxtream.core.domain.ApiKey;

/**
 * User: candide
 * Date: 12/08/13
 * Time: 09:55
 */
public interface FileUploadSupport {

    public int importFile(ApiKey apiKey, File file) throws Exception;

}
