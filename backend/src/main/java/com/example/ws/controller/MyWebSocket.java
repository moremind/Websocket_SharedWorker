package com.example.ws.controller;

import org.springframework.stereotype.Component;

import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

@ServerEndpoint(value = "/websocket")
@Component
public class MyWebSocket {
    //静态变量，用来记录当前在线连接数。应该把它设计成线程安全的。
    private static int onlineCount = 0;

    //concurrent包的线程安全Set，用来存放每个客户端对应的MyWebSocket对象。
    private static CopyOnWriteArraySet<MyWebSocket> webSocketSet = new CopyOnWriteArraySet<MyWebSocket>();

    private static ConcurrentHashMap<Session, MyWebSocket> websocketMap = new ConcurrentHashMap<>();

    //与某个客户端的连接会话，需要通过它来给客户端发送数据
    private Session session;

    private Session id;

    /**
     * 连接建立成功调用的方法*/
    @OnOpen
    public void onOpen(Session session) {
        this.session = session;
        webSocketSet.add(this);     //加入set中
        addOnlineCount();           //在线数加1
        this.id = session;
        websocketMap.put(session, this);

        System.out.println("有新连接加入！当前在线人数为" + getOnlineCount());
        try {
            sendMessage("----当前有连接进入," + getOnlineCount());
        } catch (IOException e) {
            System.out.println("IO异常");
        }
    }

    /**
     * 连接关闭调用的方法
     */
    @OnClose
    public void onClose() {
        webSocketSet.remove(this);  //从set中删除
        subOnlineCount();           //在线数减1
        System.out.println("有一连接关闭！当前在线人数为" + getOnlineCount());
    }

    /**
     * 收到客户端消息后调用的方法
     *
     * @param message 客户端发送过来的消息*/
    @OnMessage
    public void onMessage(String message, Session session) {
        System.out.println("来自客户端的消息:" + message);
        try {
            sendMessageToUser(message, session);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


     @OnError
     public void onError(Session session, Throwable error) {
         System.out.println("发生错误");
         error.printStackTrace();
     }


     public void sendMessage(String message) throws IOException {
        this.session.getBasicRemote().sendText(message);
     //this.session.getAsyncRemote().sendText(message);
     }

     public void sendMessageToUser(String message, Session session) throws IOException {
         if (websocketMap.get(session) != null) {
             if(!id.equals(session)) {
                 websocketMap.get(session).sendMessage("用户" + session + "发来消息：" + " <br/> " + message);
             } else {
                 websocketMap.get(session).sendMessage("用户1=====>" + session + "发来消息：" + " <br/> " + message);
             }
         } else {
             //如果用户不在线则返回不在线信息给自己
             sendMessageToUser("当前用户不在线",id);
         }
     }

     public void sendMessageToAll(String message) {
         for (MyWebSocket item : webSocketSet) {
             try {
                 item.sendMessage(message);
             } catch (IOException e) {
                 e.printStackTrace();
             }
         }
     }


     /**
      * 群发自定义消息
      * */
    public static void sendInfo(String message) throws IOException {
        for (MyWebSocket item : webSocketSet) {
            try {
                item.sendMessage(message);
            } catch (IOException e) {
                continue;
            }
        }
    }

    public static synchronized int getOnlineCount() {
        return onlineCount;
    }

    public static synchronized void addOnlineCount() {
        MyWebSocket.onlineCount++;
    }

    public static synchronized void subOnlineCount() {
        MyWebSocket.onlineCount--;
    }
}