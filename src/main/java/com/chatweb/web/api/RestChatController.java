package com.chatweb.web.api;


import com.alibaba.fastjson.JSONObject;
import com.chatweb.comm.PathConstant;
import com.chatweb.core.OpenAiApi;
import com.chatweb.mapper.UserContentDao;
import com.chatweb.model.ExecuteRet;
import com.chatweb.model.UserContent;
import com.chatweb.model.completion.CompletionRequest;
import com.chatweb.model.completion.chat.ChatCompletionChoice;
import com.chatweb.model.completion.chat.ChatCompletionRequest;
import com.chatweb.model.completion.chat.ChatMessage;
import com.chatweb.model.completion.chat.ChatMessageRole;
import com.chatweb.utils.DateUtils;
import com.chatweb.web.BaseController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Date;
import java.util.List;

/**
 * <p></p>
 *
 * @author wzq 2023/3/7 13:55
 * @version V1.0
 * @modificationHistory=========================逻辑或功能性重大变更记录
 * @modify by user: {修改人} 2023/3/7
 * @modify by reason:{方法名}:{原因}
 */
@RestController
public class RestChatController extends BaseController {

    @Value("${session.num}")
    private int size;
    @Value("${session.time}")
    private int sessionTime;

    @Autowired
    private UserContentDao userContentDao;

    @Autowired
    private OpenAiApi openAiApi;

    @RequestMapping("/createChatCompletion")
    public JSONObject createChatCompletion(HttpServletRequest request, HttpServletResponse response) {
        String user = getUser().getUserName();
        String content = request.getParameter("content");
        final List<ChatMessage> messages = userContentDao.selectMessageForUser(user, size, DateUtils.getBeforeByHourTime(sessionTime));
        final ChatMessage systemMessage = new ChatMessage(ChatMessageRole.USER.value(), content);
        //消息入库
        UserContent userContent = new UserContent();
        userContent.setContent(content);
        userContent.setRole(ChatMessageRole.USER.value());
        userContent.setUser(user);
        userContent.setCreateTime(new Date());
        userContentDao.insertSelective(userContent);
        messages.add(systemMessage);

        ChatCompletionRequest chatCompletionRequest = ChatCompletionRequest.builder()
                .model("gpt-3.5-turbo-0301")
                .messages(messages)
                .user("testing")
                .max_tokens(1000)
                .temperature(1.0)
                .build();

        ExecuteRet executeRet = openAiApi.post(PathConstant.COMPLETIONS.CREATE_CHAT_COMPLETION, JSONObject.toJSONString(chatCompletionRequest),
                null);
        JSONObject result = JSONObject.parseObject(executeRet.getRespStr());
        List<ChatCompletionChoice> choices = result.getJSONArray("choices").toJavaList(ChatCompletionChoice.class);
        System.out.println(choices.get(0).getMessage().getContent());
        ChatMessage context = new ChatMessage(choices.get(0).getMessage().getRole(), choices.get(0).getMessage().getContent());
        //消息入库
        UserContent aiContent = new UserContent();
        aiContent.setContent(context.getContent());
        aiContent.setRole(context.getRole());
        aiContent.setUser(user);
        aiContent.setCreateTime(new Date());
        userContentDao.insertSelective(aiContent);
        return JSONObject.parseObject(JSONObject.toJSONString(context));
    }

    @RequestMapping("/createCompletion")
    public JSONObject createCompletion(HttpServletRequest request, HttpServletResponse response) {
        CompletionRequest completionRequest = CompletionRequest.builder()
                .model("gpt-3.5-turbo")
                .prompt("Somebody once told me the world is gonna roll me")
                .user("testing")
                .max_tokens(50)
                .build();
        ExecuteRet executeRet = openAiApi.post(PathConstant.COMPLETIONS.CREATE_COMPLETION, JSONObject.toJSONString(completionRequest),
                null);
//        List<ChatCompletionChoice> choices = service.createChatCompletion(chatCompletionRequest).getChoices();
        JSONObject result = JSONObject.parseObject(executeRet.getRespStr());
        return result;
    }

} 