package BsK.client.network.handler;

public interface ResponseListener<T> {
    void onResponse(T response);
}
