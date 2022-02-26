package model.fixture;

import model.Entity;

import java.util.Objects;


public class TestEntity extends Entity {
    private int i;
    private String s;
    private double d;
    private String type;

    public TestEntity() {

    }
    public TestEntity(int i, String s, double d, String t) {
        this.i = i;
        this.s = s;
        this.d = d;
        this.type = t;
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

    public String getType() {
        return type;
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

    public void setType(String t) {
        this.type = t;
    }

    @Override
    public String type() {
        return type;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TestEntity that = (TestEntity) o;
        return i == that.i && Double.compare(that.d, d) == 0 && Objects.equals(s, that.s) && Objects.equals(type, that.type);
    }

}
