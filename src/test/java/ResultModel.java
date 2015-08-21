/**
 * Created by LambdaCat on 15/8/21.
 */
public class ResultModel {
    private boolean ok;
    private String msg;

    public ResultModel(boolean ok, String msg) {
        this.ok = ok;
        this.msg = msg;
    }

    @Override
    public String toString() {
        return "ResultModel{" +
                "ok=" + ok +
                ", msg='" + msg + '\'' +
                '}';
    }
}
