package com.models;

import com.models.market.*;
import com.models.profile.*;

public class ModelFacade {
    Market market;
    Profile profile;

    public ModelFacade(Market market, Profile profile) {
        this.market = market;
        this.profile = profile;
    }

    // make trade at current time
    public boolean makeTrade(TradeItem ti, boolean isBuy, int shares) {
        long ts = System.currentTimeMillis();
        return makeTrade(ti, isBuy, shares, ts);
    }

    public boolean makeTrade(TradeItem ti, boolean isBuy, int shares, long timestamp) {
        Order.side side;
        if (isBuy) side = Order.side.BUY;
        else side = Order.side.SELL;

        Order order = new Order(profile.getActiveAccount(), ti, side, shares, ti.getCurrentPrice(), timestamp);
        return false;
    }
}