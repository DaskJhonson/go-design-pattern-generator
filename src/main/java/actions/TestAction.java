package actions;

import com.goide.GoIcons;
import com.goide.psi.*;
import com.goide.psi.impl.GoTypeUtil;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.psi.PsiElement;
import template.DesignPattern;

import java.util.List;
import java.util.Optional;

/**
 * @author DASK
 * @date 2020/4/4 14:36
 * @description //TODO SDK API TestAction
 */
public class TestAction extends BaseAction {
    @Override
    protected void actionPerformedImpl(AnActionEvent event, Project project, GoFile file, Editor editor) {
        StringBuilder builder = new StringBuilder();
        builder.append(funcTest(file))
                .append(methodTest(file))
                .append(varTest(file))
                .append(constTest(file))
                .append(strucTest(file))
                .append(interfaceTest(file));
        Messages.showMessageDialog(project, builder.toString(), "Test", GoIcons.ICON);
    }

    private void parseParametersList(GoParameters p, StringBuilder builder) {
        if (p == null) return;
        for (GoParamDefinition definition : p.getDefinitionList()) {
            //参数名
            builder.append(definition.getName())
                    .append("=>")
                    //参数类型
                    .append(Optional.ofNullable(definition.getGoType(null))
                            .map(PsiElement::getText).orElse(""))
                    .append("\n");
        }
    }

    //获取所有的函数
    private StringBuilder funcTest(GoFile file) {
        StringBuilder builder = new StringBuilder();
        builder.append("Func:\n");
        for (GoFunctionDeclaration declaration : file.getFunctions()) {
            //方法名
            builder.append("FuncName:\n").append(declaration.getName()).append("\n");
            //获取函数参数列表
            builder.append("Parameters:\n");
            Optional.ofNullable(declaration.getSignature().getParameters()).ifPresent((p) -> {
                parseParametersList(p, builder);
            });
            builder.append("Results:\n");
            //返回的值列表
            Optional.ofNullable(declaration.getSignature().getResult())
                    .filter((p) -> p.getParameters() != null)
                    .ifPresent((p) -> {
                        if (!p.isVoid()) {
                            parseParametersList(p.getParameters(), builder);
                        } else {/*单参数无名返回值*/
                            builder.append(declaration.getSignature().getResultType().getText());
                        }
                    });
            builder.append("\n----------\n");
        }
        builder.append("\n");
        return builder;
    }

    //获取所有方法
    private StringBuilder methodTest(GoFile file) {
        StringBuilder builder = new StringBuilder();
        builder.append("Method:\n");
        for (GoMethodDeclaration declaration : file.getMethods()) {
            //方法名
            builder.append("MethodName:").append(declaration.getName()).append("\n");
            //获取函数参数列表
            builder.append("Parameters:\n");
            Optional.ofNullable(declaration.getSignature().getParameters()).ifPresent((p) -> {
                parseParametersList(p, builder);
            });
            builder.append("Results:\n");
            //返回的值列表
            Optional.ofNullable(declaration.getSignature().getResult())
                    .filter((p) -> p.getParameters() != null)
                    .ifPresent((p) -> {
                        if (!p.isVoid()) {
                            parseParametersList(p.getParameters(), builder);
                        } else {/*单参数无名返回值*/
                            builder.append(declaration.getSignature().getResultType().getText())
                                    .append("\n");
                        }
                    });
            builder.append("\n----------\n");
        }
        builder.append("\n");
        return builder;
    }

    private StringBuilder varTest(GoFile file) {
        StringBuilder builder = new StringBuilder();
        builder.append("Var:\n");
        for (GoVarDefinition declaration : file.getVars()) {
            /*变量名和值*/
            builder.append(declaration.getName());
            builder.append("=")
                    .append(declaration.getValue())
                    .append("\n");
        }
        builder.append("\n");
        return builder;
    }

    private StringBuilder constTest(GoFile file) {
        StringBuilder builder = new StringBuilder();
        builder.append("Const:\n");
        for (GoConstDefinition declaration : file.getConstants()) {
            /*变量名和值*/
            builder.append(declaration.getName());
            builder.append("=")
                    .append(declaration.getValue())
                    .append("\n");
        }
        builder.append("\n");
        return builder;
    }

    private StringBuilder strucTest(GoFile file) {
        StringBuilder builder = new StringBuilder();
        builder.append("Struct:\n");
        for (GoTypeSpec type : file.getTypes()) {
            if (GoTypeUtil.isInterface(type)) continue;
            /*强转可得结构体对象*/
            GoStructType structType = (GoStructType) type.getSpecType().getType();
            builder.append("StructName:").append(type.getName()).append("\n");
            builder.append("fields:\n");
            Optional.ofNullable(structType)
                    .map(GoStructType::getFieldDeclarationList)
                    .ifPresent((fields) -> {
                        for (GoFieldDeclaration declaration : fields) {
                            builder.append("[");
                            //获取变量名和类型
                            for (GoFieldDefinition definition : declaration.getFieldDefinitionList())
                                builder.append(definition.getName()).append(",");
                            builder.append("]");
                            //获取变量类型
                            builder.append("=>")
                                    .append(Optional.ofNullable(declaration.getType())
                                            .map(PsiElement::getText)
                                            .orElse("")
                                    )
                                    .append(",tag:")
                                    .append(declaration.getTagText())
                                    .append("\n");
                        }
                    });
            builder.append("--------\n");
        }
        builder.append("\n");
        return builder;
    }

    private StringBuilder interfaceTest(GoFile file) {
        StringBuilder builder = new StringBuilder();
        builder.append("interface:\n");
        for (GoTypeSpec type : file.getTypes()) {
            if (GoTypeUtil.isInterface(type)) {
                builder.append("InterfaceName:\n").append(type.getName()).append("\n");
                List<DesignPattern.MethodInfo> lists = DesignPattern.MethodInfo.Parse(type);
                builder.append("methods:\n").append(type.getName()).append("\n");
                for (DesignPattern.MethodInfo methodInfo : lists) {
                    builder.append("[")
                            .append(methodInfo.getName())
                            .append("=>")
                            .append(methodInfo.getParametersText())
                            .append("=>")
                            .append(methodInfo.getResultsText())
                            .append("]\n");
                }
            }
        }
        builder.append("\n");
        return builder;
    }
}
