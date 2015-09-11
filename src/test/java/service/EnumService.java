package service;

import model.Color;
import model.Version;

/**
 * 返回和参数是枚举
 * Created by LambdaCat on 15/9/12.
 */
public interface EnumService {
    public Color getColor();

    public Color shiftColor(Color x);

    public Version shiftVersion(Version x);
}
