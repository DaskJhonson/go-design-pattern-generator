package actions;

import com.goide.psi.GoFile;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import template.pattern.ProxyTemplate;
import utils.PopupUtil;

/**
 * @author DASK
 * @date 2020/4/4 19:14
 * @description //TODO 代理模式操作
 */
public class ProxyAction extends BaseAction {
    @Override
    protected void actionPerformedImpl(AnActionEvent event, Project project, GoFile file, Editor editor) {
        PopupUtil.getChooseStructPopup(file, editor, project, "Choose struct to be proxy", structType -> {
            PopupUtil.getChooseInterfacePopup(file, project, "Choose which interface to implement proxy pattern", interfaceToImpl -> {
                new ProxyTemplate(event,structType,interfaceToImpl).generateText();
            });
        });
    }
}
