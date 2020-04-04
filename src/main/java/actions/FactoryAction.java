package actions;

import com.goide.psi.GoFile;
import com.goide.psi.GoTypeSpec;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import template.pattern.BuilderTemplate;
import template.pattern.FactoryTemplate;
import utils.NotificationUtil;
import utils.PopupUtil;

/**
 * @author DASK
 * @date 2020/3/27 15:54
 * @description //TODO 工厂模式操作
 */
public class FactoryAction extends BaseAction {
    @Override
    protected void actionPerformedImpl(AnActionEvent event,Project project, GoFile file, Editor editor) {
        PopupUtil.getMultiChooseStructPopup(file, editor, project,null, list -> {
            if (list==null) {
                NotificationUtil.notifyWarn(project,"Please select at least 2 structures!\nTip:Use Shift+(Left Click)");
                return;
            }

            if (list.isEmpty()) {
                NotificationUtil.notifyWarn(project,"Please right click at blank line and select");
                return;
            }

            PopupUtil.getChooseInterfacePopup(file, project,null, interfaceToImpl -> new FactoryTemplate(event,list,interfaceToImpl).generateText());
        });
    }
}
