package model.fixture;

import java.util.Objects;

import model.Entity;

/**
 * Represents a test entity.
 */
public class TestEntity extends Entity {
  private int testInt;
  private String testString;
  private double testDouble;
  private String testType;

  public TestEntity() {

  }

  /**
   * Constructor of a TestEntity.
   *
   * @param testInt dummy int variable.
   * @param testString dummy String variable.
   * @param testDouble dummy double variable.
   * @param testType dummy String variable.
   */
  public TestEntity(int testInt, String testString, double testDouble, String testType) {
    this.testInt = testInt;
    this.testString = testString;
    this.testDouble = testDouble;
    this.testType = testType;
  }

  public int getTestInt() {
    return testInt;
  }

  public void setTestInt(int testInt) {
    this.testInt = testInt;
  }

  public String getTestString() {
    return testString;
  }

  public void setTestString(String testString) {
    this.testString = testString;
  }

  public double getTestDouble() {
    return testDouble;
  }

  public void setTestDouble(double testDouble) {
    this.testDouble = testDouble;
  }

  public String getTestType() {
    return testType;
  }

  public void setTestType(String testType) {
    this.testType = testType;
  }

  @Override
  public String type() {
    return testType;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    TestEntity that = (TestEntity) o;
    boolean intEquals = testInt == that.testInt;
    int doubleEquals = Double.compare(that.testDouble, testDouble);
    boolean stringEquals = Objects.equals(testString, that.testString);
    boolean typeEquals = Objects.equals(testType, that.testType);
    return  intEquals && doubleEquals == 0 && stringEquals && typeEquals;
  }

  @Override
  public int hashCode() {
    return Objects.hash(testInt, testString, testDouble, testType);
  }
}
