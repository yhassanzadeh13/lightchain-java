package networking;

import model.Entity;
import model.exceptions.LightChainNetworkingException;

public class StubNetworkThread extends Thread{
    StubNetwork stubNetworkT;
    StubNetwork stubNetworkR;
    Entity entity;
    String ch;

    public StubNetworkThread(StubNetwork stubNetworkT, StubNetwork stubNetworkR, Entity entity, String ch) {
        this.stubNetworkT = stubNetworkT;
        this.stubNetworkR = stubNetworkR;
        this.entity = entity;
        this.ch = ch;
    }

    @Override
    public void run() {
        try {
            System.out.println(ch);

            stubNetworkT.sendUnicast(ch,stubNetworkR,entity);
        } catch (LightChainNetworkingException e) {
            e.printStackTrace();
        }

    }
}
