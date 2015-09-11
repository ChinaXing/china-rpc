package model;

/**
 * Created by LambdaCat on 15/9/12.
 */
public enum Version {
    V1(1, "V1"),
    V2(2, "V2"),
    V3(4, "V3");

    private int index;
    private String name;

    Version(int index, String name) {
        this.index = index;
        this.name = name;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
