package actions;

import com.goide.GoIcons;
import com.goide.configuration.GoUIUtil;
import com.goide.psi.*;
import com.goide.psi.impl.GoPsiImplUtil;
import com.goide.psi.impl.GoTypeUtil;
import com.goide.refactor.util.GoRefactoringUtil;
import com.google.gson.Gson;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.ResolveState;
import com.intellij.vcs.log.ui.actions.OpenAnotherLogTabAction;
import template.DesignPattern;
import utils.PopupUtil;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * @author DASK
 * @date 2020/4/4 14:36
 * @description //TODO
 */
public class TestAction extends BaseAction {
    @Override
    protected void actionPerformedImpl(AnActionEvent event, Project project, GoFile file, Editor editor) {
        PopupUtil.getChooseInterfacePopup(file, project,null,interfaceType -> {
            StringBuilder builder=new StringBuilder();
            builder.append(interfaceType.isValid());
            for (GoNamedSignatureOwner signatureOwner : interfaceType.getAllMethods()) {
                builder.append(signatureOwner.getName()).append("\n");
                GoSignature signature = signatureOwner.getSignature();
                Optional.ofNullable(signature)
                        .map(GoSignature::getParameters)
                        .ifPresent(goParameters -> {
                            for (GoParamDefinition definition : goParameters.getDefinitionList()) {
                                builder.append(definition.getName()).append("=>").append(definition.getGoType(null).getText()).append("\n");
                            }
                        });
                Optional.ofNullable(signature)
                        .map(GoSignature::getResult)
                        .ifPresent((result) -> {
                            List<DesignPattern.FieldInfo> fieldInfos=new ArrayList<>();
                            if (!result.isVoid()) {
                                Optional.ofNullable(result.getParameters())
                                        .ifPresent(p -> {
                                            for (GoParamDefinition definition : p.getDefinitionList()) {
                                                builder.append(definition.getName()).append("=>").append(definition.getGoType(null).getText()).append("\n");
                                            }
                                        });
                                //为空说明是单参数

                            }else{

                            }
                        });
                builder.append(signatureOwner.getResultType().getText()).append("\n");
            }
            Messages.showMessageDialog(project,builder.toString(),"Test", GoIcons.ICON);
        });
    }
}
