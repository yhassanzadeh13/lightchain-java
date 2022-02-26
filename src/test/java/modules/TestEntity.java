package modules;

import model.Entity;

public class TestEntity extends Entity {
    private int i;
    private String s;
    private double d;
    private String type;

    public TestEntity(int i, String s, double d) {
        this.i = i;
        this.s = s;
        this.d = d;
    }

    public int getI() {
        return i;
    }

    public String getS() {
        return s;
    }

    public double getD() {
        return d;
    }

    public void setI(int i) {
        this.i = i;
    }

    public void setS(String s) {
        this.s = s;
    }

    public void setD(double d) {
        this.d = d;
    }
    @Override
    public String type() {
        return null;
    }
}
