package utils;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationDisplayType;
import com.intellij.notification.NotificationGroup;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.project.Project;

/**
 * @author DASK
 * @date 2020/3/30 16:14
 * @description //TODO 非模态消息提示框
 */
public class NotificationUtil {
    private final static NotificationGroup NOTIFICATION_GROUP=new NotificationGroup("Go DesignPattern Generator", NotificationDisplayType.BALLOON, false);

    //错误消息提示框,
    public static void notifyError(Project project, String content){
        final Notification notification = NOTIFICATION_GROUP.createNotification(content, NotificationType.ERROR);
        notification.notify(project);
    }

    //普通消息提示框
    public static void notifyInfo(Project project,String content){
        final Notification notification = NOTIFICATION_GROUP.createNotification(content, NotificationType.INFORMATION);
        notification.notify(project);
    }

    //警告消息提示框
    public static void notifyWarn(Project project,String content){
        final Notification notification = NOTIFICATION_GROUP.createNotification(content, NotificationType.WARNING);
        notification.notify(project);
    }
}
