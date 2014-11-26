package glacier.freshbooks;

import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.URL;

import oauth.signpost.OAuthConsumer;
import oauth.signpost.basic.DefaultOAuthConsumer;
import oauth.signpost.signature.PlainTextMessageSigner;

import org.apache.commons.io.IOUtils;
import org.fluxtream.core.domain.ApiKey;
import org.springframework.stereotype.Component;

import org.fluxtream.core.TimeInterval;
import org.fluxtream.core.connectors.annotations.Updater;
import org.fluxtream.core.connectors.updaters.AbstractUpdater;
import org.fluxtream.core.connectors.updaters.UpdateInfo;

/**
 * 
 * @author candide
 * 
 */

@Component
@Updater(prettyName = "Freshbooks", value = 36, objectTypes ={})
public class FreshbooksUpdater extends AbstractUpdater {

	public FreshbooksUpdater() {
		super();
	}

    @Override
    protected void updateConnectorDataHistory(final UpdateInfo updateInfo) throws Exception {
        throw new RuntimeException("Not Yet Implemented");
    }

	TimeInterval timeInterval;
	
	@Override
	public void updateConnectorData(UpdateInfo updateInfo) throws Exception {
		URL url = new URL("https://palacehotelsoftware.freshbooks.com/api/2.1/xml-in");

		HttpURLConnection request = (HttpURLConnection) url
				.openConnection();

		OAuthConsumer consumer = new DefaultOAuthConsumer(
				env.get("freshbooksConsumerKey"),
				env.get("freshbooksConsumerSecret"));
		consumer.setMessageSigner(new PlainTextMessageSigner());
		consumer.setTokenWithSecret(guestService.getApiKeyAttribute(updateInfo.apiKey,"accessToken"),
                                    guestService.getApiKeyAttribute(updateInfo.apiKey,"tokenSecret"));

		consumer.sign(request);
		
		request.setDoOutput(true);
		Writer requestWriter = new OutputStreamWriter(
				request.getOutputStream());

		requestWriter
				.write("<!--?xml version=\"1.0\" encoding=\"utf-8\"?-->"
						+ "<request method=\"project.list\">  "
						+ "<!-- The page number to show (Optional) -->  "
						+ "<page>1</page>  "
						+ "<!-- Number of results per page, default 25 (Optional) -->  "
						+ "<per_page>15</per_page>  " + "</request>  ");

		requestWriter.flush();
		
		if (request.getResponseCode() == 200) {
			String xml = IOUtils.toString(request.getInputStream());
			apiDataService.cacheApiDataXML(updateInfo,
					xml, -1, -1);
		} else {
			throw new Exception("Unexpected response code: " + request.getResponseCode());
		}
	}

    @Override
    public void setDefaultChannelStyles(ApiKey apiKey) {}

}
