package actions;

import com.goide.psi.*;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import com.intellij.psi.ResolveState;
import org.jetbrains.annotations.NotNull;
import utils.NotificationUtil;

import java.util.Optional;

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

    private void FucTest(StringBuilder builder,GoFile file){
        builder.append("FucTest:\n");
        for (GoFunctionDeclaration declaration : file.getFunctions()) {
            /*方法名*/
            builder.append(declaration.getName());
            builder.append("\n");
            /*参数名和类型*/
            Optional.ofNullable(declaration.getSignature().getParameters()).ifPresent((p) -> {
                for (GoParamDefinition definition : p.getDefinitionList()) {
                    builder.append("[").append(definition.getName())
                            .append("=>");
                    Optional.ofNullable(definition.getGoType(ResolveState.initial()))
                            .ifPresent((t) -> builder.append(t.getText()).append("]"));
                }
                builder.append(",");
            });
            builder.append("\n");
            /*返回的值的名称和类型名*/
            Optional.ofNullable(declaration.getSignature().getResult())
                    .filter((p) -> p.getParameters() != null)
                    .ifPresent((p) -> {
                        for (GoParamDefinition definition : p.getParameters().getDefinitionList()) {
                            builder.append("[").append(definition.getName())
                                    .append("=>");
                            Optional.ofNullable(definition.getGoType(ResolveState.initial()))
                                    .ifPresent((t) -> builder.append(t.getText()).append("]"));
                        }
                        builder.append(",");
                    });
            builder.append("\n");
            /*返回值类型名*/
            Optional.ofNullable(declaration.getSignature().getResultType()).ifPresent(
                    (p) -> builder.append(p.getText()).append(",")
            );

            builder.append("\n\n");
        }
        builder.append("\n");
    }

    private void MethodTest(StringBuilder builder,GoFile file){
        builder.append("MethodTest:\n");
        for (GoMethodDeclaration declaration : file.getMethods()) {
            /*方法名*/
            builder.append(declaration.getName());
            builder.append("\n");
            Optional.ofNullable(declaration.getReceiver()).ifPresent((r)->{
                /*所属对象名*/
                builder.append(r.getName()).append("=>");
                /*所属对象类型名*/
                builder.append(declaration.getReceiverType().getText()).append(",");
            });
            builder.append("\n\n");
        }
        builder.append("\n");
    }

    private void VarTest(StringBuilder builder,GoFile file){
        for (GoVarDefinition declaration : file.getVars()) {
            /*变量名和值*/
            builder.append(declaration.getName());
            builder.append("=")
                    .append(declaration.getValue())
                    .append("\n\n");
        }
        builder.append("\n");
    }

    private void StrucTest(StringBuilder builder,GoFile file){
        builder.append("MethodTest:\n");
        for (GoTypeSpec type:file.getTypes()){
            /*强转可得结构体对象*/
            GoStructType structType=(GoStructType) type.getSpecType().getType();
            for (GoFieldDeclaration declaration:structType.getFieldDeclarationList()){
                //获取变量名和类型
                for (GoFieldDefinition definition:declaration.getFieldDefinitionList()) builder.append(definition.getName()).append(",");
                //获取变量类型
                builder.append("=>")
                        .append(declaration.getType().getText())
                        .append("-")
                        .append(declaration.getTagText());//获取标签值
            }
        }
    }

//    GoImplementMethodsHandler对象选择框PsiElementListNavigator.navigateOrCreatePopup()
//    GoGenerateConstructorIntention 成员选择框GoMemberChooser
//    GoImplementMethodsHandler.MyChooseByNamePopup方法选择器
}
