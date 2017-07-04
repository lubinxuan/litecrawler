package me.robin.crawler.push;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.lmax.disruptor.EventFactory;
import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.YieldingWaitStrategy;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;
import org.apache.commons.lang3.StringUtils;

import java.util.concurrent.Executors;

/**
 * Created by LubinXuan on 2017/7/5.
 */
public class CrawlerDataDisruptor {

    public class CrawlerDataEvent {
        private String data;

        public void setData(String data) {
            this.data = data;
        }
    }

    private class CrawlerDataEventFactory implements EventFactory<CrawlerDataEvent> {
        @Override
        public CrawlerDataEvent newInstance() {
            return new CrawlerDataEvent();
        }
    }


    private class CrawlerDataEventHandler implements EventHandler<CrawlerDataEvent> {
        @Override
        public void onEvent(CrawlerDataEvent crawlerDataEvent, long l, boolean b) throws Exception {

            if (StringUtils.isBlank(crawlerDataEvent.data)) {
                return;
            }
            try {
                //todo upload data;
                JSONObject data = JSON.parseObject(crawlerDataEvent.data);
            } finally {
                crawlerDataEvent.setData(null);
            }
        }
    }


    public static final CrawlerDataDisruptor ins = new CrawlerDataDisruptor();

    private Disruptor<CrawlerDataEvent> disruptor;

    private CrawlerDataDisruptor() {
        EventFactory<CrawlerDataEvent> eventFactory = new CrawlerDataEventFactory();
        int ringBufferSize = 2 ^ 14; // RingBuffer 大小，必须是 2 的 N 次方；
        disruptor = new Disruptor<>(eventFactory,
                ringBufferSize, Executors.defaultThreadFactory(), ProducerType.MULTI,
                new YieldingWaitStrategy());
        EventHandler eventHandler = new CrawlerDataEventHandler();
        disruptor.handleEventsWith(eventHandler);
        disruptor.start();
    }

    public void pushData(String jsonData) {
        RingBuffer<CrawlerDataEvent> ringBuffer = disruptor.getRingBuffer();
        long sequence = ringBuffer.next();
        try {
            CrawlerDataEvent event = ringBuffer.get(sequence);
            event.setData(jsonData);
        } finally {
            ringBuffer.publish(sequence);
        }
    }
}
