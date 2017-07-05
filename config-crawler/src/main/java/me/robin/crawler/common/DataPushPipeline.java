package me.robin.crawler.common;

import com.alibaba.fastjson.JSON;
import me.robin.crawler.Param;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import us.codecraft.webmagic.ResultItems;
import us.codecraft.webmagic.Task;
import us.codecraft.webmagic.pipeline.Pipeline;

import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Created by Lubin.Xuan on 2017-07-05.
 */
public class DataPushPipeline implements Pipeline {

    private static final Logger logger = LoggerFactory.getLogger(DataPushPipeline.class);

    private static final HttpClient client = HttpClients.createDefault();

    public static final String DATA_LIST = "DATA_LIST";

    private HttpHost httpHost = new HttpHost("127.0.0.1", 8080);

    private final String sourceType;

    public DataPushPipeline(String sourceType) {
        this.sourceType = sourceType;
    }

    public DataPushPipeline(String sourceType, String host, int port) {
        this(sourceType);
        this.httpHost = new HttpHost(host, port);
    }

    @Override
    public void process(ResultItems resultItems, Task task) {
        List<Map<String, Object>> dataList = resultItems.get(DATA_LIST);
        if (null != dataList) {
            for (Map<String, Object> data : dataList) {
                String dataType = (String) data.remove(Param.dataType);
                this.push(dataType, data);
            }
        } else {
            Map<String, Object> data = resultItems.getAll();
            String dataType = (String) data.remove(Param.dataType);
            this.push(dataType, data);
        }
    }

    //数据提交死循环至完成提交服务器
    private void push(String dataType, Map<String, Object> data) {
        HttpPost post = new HttpPost("/push/data");
        post.setEntity(new StringEntity(JSON.toJSONString(data), Charset.forName("utf-8")));
        post.setHeader("data-type", dataType);
        post.setHeader("source-type", sourceType);
        HttpResponse response = null;
        Throwable throwable = null;
        int statusCode = 0;
        int log = 0;
        while (true) {
            try {
                response = client.execute(httpHost, post);
                if (response.getStatusLine().getStatusCode() == 200) {
                    return;
                }
                statusCode = response.getStatusLine().getStatusCode();
            } catch (Exception e) {
                throwable = e;
            } finally {
                if (null != response) {
                    EntityUtils.consumeQuietly(response.getEntity());
                }
                post.releaseConnection();
            }
            if (log++ % 5 == 0) {
                if (null != throwable) {
                    logger.warn("数据上传异常,Exception:{}", throwable.getMessage());
                } else {
                    logger.warn("数据上传返回码异常:{}", statusCode);
                }
            }
            try {
                TimeUnit.SECONDS.sleep(2);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

    }
}