package com.kgr.repChain.core;

import com.alibaba.fastjson2.JSON;
import com.kgr.repChain.entity.Jks;
import com.kgr.repChain.entity.UpChainInfo;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpEntity;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContexts;
import org.apache.http.util.EntityUtils;
import org.springframework.http.HttpMethod;

import javax.net.ssl.SSLContext;
import java.io.File;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * @author kgr
 * @create 2022-08-29 11:33
 */
@AllArgsConstructor
@Slf4j
public class ChainConnectorManager {


    public String createConnector(UpChainInfo upChainInfo) throws Exception{
        return this.createConnector(upChainInfo, null, null);
    }

    public String createConnector(UpChainInfo upChainInfo, Jks keyJks, Jks trustJks) throws Exception{

        return createRequest(HttpMethod.POST, upChainInfo, keyJks, trustJks);
    }


    private String createRequest(HttpMethod httpMethod, UpChainInfo upChainInfo, Jks keyJks, Jks trustJks) throws Exception {

        // 默认http
        String url = upChainInfo.getUrl();

        if(!url.startsWith("http") && !url.startsWith("https")) {
            url = "http://" + url;
        }

        boolean isHttps = !Objects.isNull(keyJks) && !Objects.isNull(trustJks);

        if(isHttps) {
            url = url.replace("http", "https");
        }

        log.info("=======> {}", url);

        HttpClientBuilder clientBuilder = HttpClients.custom();

        if(!Objects.isNull(keyJks) && !Objects.isNull(trustJks)) {
            SSLContext sslContext = SSLContexts.custom()
                    .loadTrustMaterial(new File(trustJks.getPath()), trustJks.getPassword().toCharArray(), new TrustSelfSignedStrategy())
                    .loadKeyMaterial(new File(keyJks.getPath()),  keyJks.getPassword().toCharArray(),  keyJks.getPassword().toCharArray())
                    .build();

            clientBuilder.setSSLContext(sslContext).setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE);
        }

        CloseableHttpClient httpClient = clientBuilder.build();

        RequestConfig config = RequestConfig.custom().setSocketTimeout(10000).setConnectTimeout(10000).build();

        CloseableHttpResponse response;

        URI uri = URI.create(url);

        if(httpMethod.equals(HttpMethod.POST)) {
            HttpPost httpPost = new HttpPost(uri);
            httpPost.setConfig(config);

            httpPost.setEntity(new StringEntity(JSON.toJSONString(upChainInfo), StandardCharsets.UTF_8));
            httpPost.setHeader("Content-Type", "application/json");
            // 发送Post请求
            response = httpClient.execute(httpPost);

        }else
        {
            HttpGet httpGet = new HttpGet(uri);
            httpGet.setConfig(config);
            response = httpClient.execute(httpGet);
        }

        // 获取响应
        HttpEntity responseEntity = response.getEntity();
        // 将响应转换为字符串
        String res = EntityUtils.toString(responseEntity, StandardCharsets.UTF_8);
        EntityUtils.consumeQuietly(responseEntity);
        return res;
    }

    public String queryData(UpChainInfo upChainInfo, Jks keyJks, Jks trustJks) throws Exception{


        return createRequest(HttpMethod.GET, upChainInfo, keyJks, trustJks);
    }

    public String queryData(UpChainInfo upChainInfo) throws Exception{
        return queryData(upChainInfo, null, null);
    }
}
