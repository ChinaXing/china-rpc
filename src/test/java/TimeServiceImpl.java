import java.util.Date;

/**
 * Created by LambdaCat on 15/8/21.
 */
public class TimeServiceImpl implements TimeService {
    public ResultModel getTimestamp() {
        return new ResultModel(true, String.valueOf(System.currentTimeMillis()));
    }

    public Date getDate() {
        return new Date();
    }
}
