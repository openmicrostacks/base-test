package de.a9d3.testing.resource_classes;

import java.util.Objects;

public class HashCodeAndEqualsCheckSameObjectTestClass {
    private String a;
    private boolean b;
    private Boolean c;

    public String getA() {
        return a;
    }

    public void setA(String a) {
        this.a = a;
    }

    public boolean isB() {
        return b;
    }

    public void setB(boolean b) {
        this.b = b;
    }

    public Boolean getC() {
        return c;
    }

    public void setC(Boolean c) {
        this.c = c;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return false; // return false instead of true
        if (o == null || getClass() != o.getClass()) return false;
        HashCodeAndEqualsCheckSameObjectTestClass that = (HashCodeAndEqualsCheckSameObjectTestClass) o;
        return b == that.b &&
                Objects.equals(a, that.a) &&
                Objects.equals(c, that.c);
    }

    @Override
    public int hashCode() {
        return Objects.hash(a, b, c);
    }
}
