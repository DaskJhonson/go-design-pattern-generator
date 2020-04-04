package utils;

import com.goide.go.GoGotoSuperHandler;
import com.goide.go.GoGotoUtil;
import com.goide.go.GoTypeContributor;
import com.goide.intentions.generate.constructor.GoMemberChooser;
import com.goide.intentions.generate.constructor.GoMemberChooserNode;
import com.goide.psi.*;
import com.goide.psi.impl.GoTypeUtil;
import com.goide.refactor.GoImplementMethodsHandler;
import com.goide.refactor.GoTypeContributorsBasedGotoByModel;
import com.goide.util.GoUtil;
import com.intellij.codeInsight.daemon.impl.PsiElementListNavigator;
import com.intellij.codeInsight.navigation.BackgroundUpdaterTask;
import com.intellij.icons.AllIcons;
import com.intellij.ide.util.gotoByName.*;
import com.intellij.navigation.ChooseByNameContributor;
import com.intellij.navigation.NavigationItem;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.wm.IdeFocusManager;
import com.intellij.psi.*;
import com.intellij.psi.impl.RenameableFakePsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.*;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.indexing.FindSymbolParameters;
import com.intellij.util.ui.UIUtil;
import one.util.streamex.StreamEx;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import template.DesignPattern;

import javax.swing.*;
import java.util.List;
import java.util.Set;

/**
 * @author DASK
 * @date 2020/3/29 20:45
 * @description //TODO 各类选择弹窗工具包
 */
public class PopupUtil {

    //回调返回得到的元素
    public interface Callback{
        void getChooseGoType(GoTypeSpec goTypeSpec);
    }

    public interface MultiCallback{
        void getMultiChooseGoType(List<GoTypeSpec> list);
    }

    //选择类的成员变量
    public static List<DesignPattern.FieldInfo> getChooseFieldPopup(GoTypeSpec spec, Project project,String title){
        if (spec==null
                || project==null
                || !(spec.getSpecType().getType() instanceof GoStructType)
        ) return null;

        GoStructType structType=(GoStructType) spec.getSpecType().getType();
        List<GoNamedElement> definitions = ContainerUtil.filter(structType.getFieldDefinitions(), (fd) -> !fd.isBlank());
        GoMemberChooserNode[] chooserNodes = ContainerUtil.map2Array(definitions, GoMemberChooserNode.class, GoMemberChooserNode::new);

        GoMemberChooser memberChooser = new GoMemberChooser(chooserNodes, project, null);
        memberChooser.setTitle(title==null?"Select Fields":title);
        SmartList<DesignPattern.FieldInfo> list=new SmartList<>();
        if (memberChooser.showAndGet()){
            for (GoMemberChooserNode node:memberChooser.getSelectedElements()) {
                GoNamedElement element = ObjectUtils.tryCast(node.getPsiElement(), GoNamedElement.class);
                list.add(new DesignPattern.FieldInfo(element.getName(),element.getGoType(null).getText()));
            }
        }
        return list;
    }

    //选择多个结构体
    public static void getMultiChooseStructPopup(@NotNull GoFile file,@NotNull Editor editor,@NotNull Project project,String title,@NotNull MultiCallback callback) {
        GoTypeSpec typeSpecToGenerate = findTypeSpec(editor, file);
        if (isValidTypeSpec(typeSpecToGenerate)) {
            //只选择一个结构体不处理
            callback.getMultiChooseGoType(new SmartList<>());
        } else {
            StreamEx<NavigatablePsiElement> validTypeSpecs = selectValidTypeSpecs(file);
            if (!ApplicationManager.getApplication().isUnitTestMode()) {
                validTypeSpecs = validTypeSpecs.prepend(new PopupUtil.CreateTypeFakePsiElement(file));
            }
            NavigatablePsiElement[] elements = validTypeSpecs.toArray(NavigatablePsiElement[]::new);
            String DEFAULT_STRUCT_TITLE = "Choose Type";
            JBPopup popup = PsiElementListNavigator.navigateOrCreatePopup(elements,title==null? DEFAULT_STRUCT_TITLE :title, null, GoGotoUtil.DEFAULT_RENDERER, ApplicationManager.getApplication().isUnitTestMode() ? null : new PopupUtil.DummyBackgroundUpdaterTask(project), (objects) -> {
                if (objects.length > 1) {
                    SmartList<GoTypeSpec> list=new SmartList<>();
                    for (Object o:objects) list.add((GoTypeSpec) o);
                    callback.getMultiChooseGoType(list);
                }else {
                    callback.getMultiChooseGoType(null);
                }
            });
            if (popup != null) {
                popup.showInBestPositionFor(editor);
            }
        }
    }

    //选择单个结构体
    public static void getChooseStructPopup(@NotNull GoFile file,@NotNull Editor editor,@NotNull Project project,String title,@NotNull Callback callback) {
        GoTypeSpec typeSpecToGenerate = findTypeSpec(editor, file);
        if (isValidTypeSpec(typeSpecToGenerate)) {
            callback.getChooseGoType(typeSpecToGenerate);
        } else {
            StreamEx<NavigatablePsiElement> validTypeSpecs = selectValidTypeSpecs(file);
            if (!ApplicationManager.getApplication().isUnitTestMode()) {
                validTypeSpecs = validTypeSpecs.prepend(new PopupUtil.CreateTypeFakePsiElement(file));
            }
            NavigatablePsiElement[] elements = validTypeSpecs.toArray(NavigatablePsiElement[]::new);
            JBPopup popup = PsiElementListNavigator.navigateOrCreatePopup(elements, title==null?"Choose Type":title, null, GoGotoUtil.DEFAULT_RENDERER, ApplicationManager.getApplication().isUnitTestMode() ? null : new PopupUtil.DummyBackgroundUpdaterTask(project), (objects) -> {
                if (objects.length == 1 ) {
                    callback.getChooseGoType((GoTypeSpec) objects[0]);
                }
            });
            if (popup != null) {
                popup.showInBestPositionFor(editor);
            }
        }
    }

    //选择要实现的接口
    public static void getChooseInterfacePopup(@NotNull GoFile file,@NotNull Project project,String title,@NotNull Callback callback){
        final GoTypeContributorsBasedGotoByModel model = new GoTypeContributorsBasedGotoByModel(project, new MyGoTypeContributor(file, null), title==null?"Choose interface to implement:":title);
        ChooseByNamePopup oldPopup = project.getUserData(ChooseByNamePopup.CHOOSE_BY_NAME_POPUP_IN_PROJECT_KEY);
        if (oldPopup != null) {
            oldPopup.close(false);
        }
        PsiDocumentManager.getInstance(project).commitAllDocuments();

        ChooseByNameItemProvider provider = new DefaultChooseByNameItemProvider(file);
        final PopupUtil.MyChooseByNamePopup popup = new PopupUtil.MyChooseByNamePopup(project, model, provider, oldPopup);
        project.putUserData(ChooseByNamePopup.CHOOSE_BY_NAME_POPUP_IN_PROJECT_KEY, popup);
        popup.setCheckBoxShortcut(ActionManager.getInstance().getAction("ImplementMethods").getShortcutSet());
        popup.setSearchInAnyPlace(true);
        popup.invoke(new ChooseByNamePopupComponent.Callback() {
            public void elementChosen(Object element) {
            }

            public void onClose() {
                Disposer.dispose(model);
                if (popup.myClosedCorrectly) {
                    Object chosenElement = popup.getChosenElement();
                    if (chosenElement instanceof GoTypeSpec && !project.isDisposed()) {
                        IdeFocusManager.getInstance(project).doWhenFocusSettlesDown(() -> {
                            callback.getChooseGoType((GoTypeSpec) chosenElement);
                        });
                    }
                }
            }
        }, ModalityState.current(), true);
    }

    private static GoTypeSpec findTypeSpec(@NotNull Editor editor,@NotNull PsiFile file) {

        Caret caret = editor.getCaretModel().getPrimaryCaret();
        int offset = caret.getOffset();
        PsiElement at = file.findElementAt(offset);
        at = !(at instanceof PsiWhiteSpace) && (at != null || offset <= 0) ? at : file.findElementAt(offset - 1);
        GoTypeSpec typeSpec = PsiTreeUtil.getParentOfType(at, GoTypeSpec.class);
        if (typeSpec != null) {
            return typeSpec;
        } else {
            GoTypeDeclaration typeDeclaration = PsiTreeUtil.getParentOfType(at, GoTypeDeclaration.class);
            if (typeDeclaration != null) {
                List<GoTypeSpec> specList = typeDeclaration.getTypeSpecList();
                if (specList.size() == 1) {
                    return specList.get(0);
                }
            }

            GoType type = PsiTreeUtil.getParentOfType(at, GoType.class);
            return type != null ? ObjectUtils.tryCast(type.contextlessResolve(), GoTypeSpec.class) : null;
        }
    }

    //将选中的结构体补充实现指定接口中的所有方法
    public static void createUnImplementMethod(GoFile file,Editor editor,GoTypeSpec structType,GoTypeSpec interfaceToImpl){
        GoImplementMethodsHandler.generateTemplate(file, editor, structType, interfaceToImpl, null);
    }

    //判断是否是个结构体
    private static boolean isValidTypeSpec(@Nullable GoTypeSpec typeSpec) {
        if (typeSpec == null) {
            return false;
        } else if (PsiTreeUtil.getParentOfType(typeSpec, GoBlock.class) != null) {
            return false;
        } else {
            GoSpecType type = typeSpec.getSpecType();
            if (type.getAssign() != null) {
                return false;
            } else {
                return !GoTypeUtil.isInterface(typeSpec);
            }
        }
    }

    private static StreamEx<NavigatablePsiElement> selectValidTypeSpecs(@NotNull GoFile file) {
        return StreamEx.of(file.getTypes()).filter(GoImplementMethodsHandler::isValidTypeSpec).select(NavigatablePsiElement.class);
    }

    private static class CreateTypeFakePsiElement extends RenameableFakePsiElement {
        private CreateTypeFakePsiElement(@NotNull PsiFile file) {
            super(file);
        }

        public String getName() {
            return "Create Type...";
        }

        public String getTypeName() {
            return this.getName();
        }

        @NotNull
        public Icon getIcon() {
            return AllIcons.Actions.IntentionBulb;
        }
    }

    private static class DummyBackgroundUpdaterTask extends BackgroundUpdaterTask {
        private DummyBackgroundUpdaterTask(Project project) {
            super(project, "Working work...", null);
        }

        public String getCaption(int size) {
            return null;
        }
    }

    private static class MyGoTypeContributor extends GoTypeContributor implements Disposable {
        private PsiFile myFile;
        private GoTypeSpec myGenerate;
        private Set<GoTypeSpec> myAlreadyImplementedTypes;

        MyGoTypeContributor(@NotNull PsiFile file, @Nullable GoTypeSpec typeSpecToGenerate) {
            super();
            this.myFile = file;
            this.myGenerate = typeSpecToGenerate;
        }

        @NotNull
        Set<GoTypeSpec> getAlreadyImplementedTypes() {
            if (this.myAlreadyImplementedTypes == null) {
                Set<GoTypeSpec> set = ContainerUtil.newTroveSet();
                if (this.myGenerate != null) {
                    GoGotoSuperHandler.SUPER_SEARCH.execute(GoGotoUtil.param(this.myGenerate), Processors.cancelableCollectProcessor(set));
                }

                this.myAlreadyImplementedTypes = set;
            }

            return this.myAlreadyImplementedTypes;
        }

        public void processElementsWithName(@NotNull String s, @NotNull Processor<NavigationItem> processor, @NotNull FindSymbolParameters parameters) {
            Set<GoTypeSpec> alreadyImplementedTypes = this.getAlreadyImplementedTypes();
            super.processElementsWithName(s, new FilteringProcessor((o) -> {
                if (!(o instanceof GoTypeSpec)) {
                    return false;
                } else if (alreadyImplementedTypes.contains(o)) {
                    return false;
                } else {
                    GoTypeSpec typeSpec = (GoTypeSpec)o;
                    return !typeSpec.isPublic() && !GoUtil.inSamePackage(this.myFile, typeSpec.getContainingFile()) ? false : GoTypeUtil.isInterface(typeSpec);
                }
            }, processor), parameters);
        }

        public void dispose() {
            this.myFile = null;
            this.myGenerate = null;
            if (this.myAlreadyImplementedTypes != null) {
                this.myAlreadyImplementedTypes.clear();
            }

            this.myAlreadyImplementedTypes = null;
        }
    }

    private static class MyChooseByNamePopup extends ChooseByNamePopup {
        private boolean myClosedCorrectly;

        private MyChooseByNamePopup(@Nullable Project project, @NotNull ChooseByNameModel model, @NotNull ChooseByNameItemProvider provider, @Nullable ChooseByNamePopup oldPopup) {
            super(project, model, provider, oldPopup, null, false, 0);
        }

        public void close(boolean isOk) {
            if (!this.checkDisposed()) {
                this.myClosedCorrectly = isOk;
            }
            super.close(isOk);
        }
    }
}
