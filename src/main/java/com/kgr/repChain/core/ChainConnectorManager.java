package com.kgr.repChain.core;

import com.alibaba.fastjson2.JSON;
import com.kgr.repChain.entity.Jks;
import com.kgr.repChain.entity.UpChainInfo;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpEntity;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContexts;
import org.apache.http.util.EntityUtils;

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
        return this.createConnector(upChainInfo, null);
    }

    public String createConnector(UpChainInfo upChainInfo, Jks jks) throws Exception{
        HttpClientBuilder clientBuilder = HttpClients.custom();

        // 默认http
        String url = upChainInfo.getUrl();

        if(!url.startsWith("http") && !url.startsWith("https")) {
            url = "http://" + url;
        }

        if(!Objects.isNull(jks)) {
            SSLContext sslContext = SSLContexts.custom()
                    .loadTrustMaterial(new File(jks.getPath()), jks.getPassword().toCharArray(), new TrustSelfSignedStrategy())
                    .loadKeyMaterial(new File(jks.getPath()),  jks.getPassword().toCharArray(),  jks.getPassword().toCharArray())
                    .build();

            clientBuilder.setSSLContext(sslContext).setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE);

            url = url.replace("http", "https");
        }

        log.info("=======> {}", url);

        CloseableHttpClient httpClient = clientBuilder.build();

        RequestConfig config = RequestConfig.custom().setSocketTimeout(10000).setConnectTimeout(10000).build();
        HttpPost httpPost = new HttpPost(URI.create(url));
        httpPost.setConfig(config);

        StringEntity entity = new StringEntity(JSON.toJSONString(upChainInfo), StandardCharsets.UTF_8);
        httpPost.setEntity(entity);
        httpPost.setHeader("Content-Type", "application/json");
        // 发送Post请求
        CloseableHttpResponse response = httpClient.execute(httpPost);
        // 获取响应
        HttpEntity responseEntity = response.getEntity();
        // 将响应转换为字符串
        String res = EntityUtils.toString(responseEntity, StandardCharsets.UTF_8);
        EntityUtils.consumeQuietly(responseEntity);
        return res;
    }


    public String queryData(UpChainInfo upChainInfo) throws Exception{

        CloseableHttpClient httpClient = HttpClients.custom().build();
        RequestConfig config = RequestConfig.custom().setSocketTimeout(10000).setConnectTimeout(10000).build();
        HttpPost httpPost = new HttpPost(URI.create(upChainInfo.getUrl()));
        httpPost.setConfig(config);
        httpPost.setHeader("Content-Type", "application/json");

        // 发送Post请求
        CloseableHttpResponse response = httpClient.execute(httpPost);
        // 获取响应
        HttpEntity responseEntity = response.getEntity();
        // 将响应转换为字符串
        String res = EntityUtils.toString(responseEntity, StandardCharsets.UTF_8);
        EntityUtils.consumeQuietly(responseEntity);
        return res;
    }
}
