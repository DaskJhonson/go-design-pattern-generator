package actions;

import com.goide.psi.GoFile;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import utils.NotificationUtil;

/**
 * @author DASK
 * @date 2020/3/30 14:41
 * @description //TODO 所有操作的抽象基类
 */
public abstract class BaseAction extends AnAction {

    @Override
    public void update(AnActionEvent e) {
        // 判断当前项目是否打开，同时当前文件是否是go文件，来决定是否显示该选项
        Project project = e.getProject();
        PsiFile file= e.getData(LangDataKeys.PSI_FILE);
        e.getPresentation().setEnabledAndVisible(project != null&&file instanceof GoFile);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project=e.getProject();
        GoFile file= (GoFile) e.getData(LangDataKeys.PSI_FILE);
        Editor editor=e.getData(LangDataKeys.EDITOR);
        if (file==null||project==null||editor==null) {
            NotificationUtil.notifyError(project,"Can't generate content,because file is not correct");
            return;
        }
        //安全的操作文本，由SDK提供的方法保证
        WriteCommandAction.runWriteCommandAction(project, () -> actionPerformedImpl(e,project,file,editor));
    }


    protected abstract void actionPerformedImpl(AnActionEvent event,Project project,GoFile file,Editor editor);
}
