package cn.bugstack.chatbot.api.application.job;

import cn.bugstack.chatbot.api.domain.ai.IOpenAI;
import cn.bugstack.chatbot.api.domain.zsxq.IZsxqApi;
import cn.bugstack.chatbot.api.domain.zsxq.model.aggregates.UnAnsweredQuestionsAggregates;
import cn.bugstack.chatbot.api.domain.zsxq.model.vo.Topics;
import com.alibaba.fastjson.JSON;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Random;

/**
 * @description: 问题任务
 * @author：hgd
 * @date: 2023-02-05
 */

@EnableScheduling
@Configuration
public class ChatbotSchedule {
    private final Logger logger = LoggerFactory.getLogger(ChatbotSchedule.class);
    @Value("${chatbot-api.groupId}")
    private String groupId;
    @Value("${chatbot-api.cookie}")
    private String cookie;
    @Resource
    private IOpenAI openAI;
    @Resource
    private IZsxqApi zsxqApi;

    /**
     * 表达式:cron.qqe2.com
     */
    @Scheduled(cron = "0/30 * * * * ?")
    public void run() {
        try {
            if (new Random().nextBoolean()) {
                logger.info("随机打烊中...");
                return;
            }

            GregorianCalendar calendar = new GregorianCalendar();
            int hour = calendar.get(Calendar.HOUR_OF_DAY);
            if (hour > 22 || hour < 7) {
                logger.info("打烊时间不工作, AI下班了!");
                return;
            }

            // 1. 检索问题
            UnAnsweredQuestionsAggregates unAnsweredQuestionsAggregates = zsxqApi.queryUnAnsweredQuestionsTopicId(groupId, cookie);
            logger.info("检索结果:{}", JSON.toJSONString(unAnsweredQuestionsAggregates));
            List<Topics> topics = unAnsweredQuestionsAggregates.getResp_data().getTopics();
            if (null == topics || topics.isEmpty()) {
                logger.info("本次检索未查询到待回答问题");
                return;
            }
            // 2. AI回答
            Topics topic = topics.get(0);
            String answer = openAI.doChatGPT(topic.getQuestion().getText().trim());

            // 3. 问题回复
            boolean status = zsxqApi.answer(groupId, cookie, topic.getTopic_id(), answer, false);
            logger.info("编号: {} 问题: {} 回答: {} 状态: {}", topic.getTopic_id(), topic.getQuestion().getText(), answer, status);
        } catch (IOException e) {
            logger.error("自动回答问题异常", e);
        }
    }

}
