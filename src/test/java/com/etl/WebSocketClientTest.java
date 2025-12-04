package com.etl;

import com.etl.finnhub.WebSocketClient;
import jakarta.websocket.RemoteEndpoint;
import jakarta.websocket.Session;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class WebSocketClientTest {

    @Test
    public void unsubscribe_sendsUnsubscribeMessage() throws Exception {
        WebSocketClient client = new WebSocketClient("dummy");

        Session mockSession = mock(Session.class);
        RemoteEndpoint.Async mockAsync = mock(RemoteEndpoint.Async.class);
        when(mockSession.getAsyncRemote()).thenReturn(mockAsync);
        when(mockSession.isOpen()).thenReturn(true);

        // Inject mock session into private field
        Field sessionField = WebSocketClient.class.getDeclaredField("session");
        sessionField.setAccessible(true);
        sessionField.set(client, mockSession);

        client.unsubscribe("AAPL");

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(mockAsync, times(1)).sendText(captor.capture());
        String sent = captor.getValue();
        assertTrue(sent.contains("unsubscribe"));
        assertTrue(sent.contains("AAPL"));
    }
}
