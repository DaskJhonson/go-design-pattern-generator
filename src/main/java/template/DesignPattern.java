package template;

import com.goide.GoTypes;
import com.goide.psi.*;
import com.goide.psi.impl.GoPsiImplUtil;
import com.goide.psi.impl.GoTypeUtil;
import com.goide.refactor.template.GoTemplate;
import com.goide.refactor.util.GoRefactoringUtil;
import com.intellij.codeInsight.actions.OptimizeImportsAction;
import com.intellij.codeInsight.actions.ReformatCodeAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiRecursiveElementWalkingVisitor;
import com.intellij.psi.impl.source.tree.LeafPsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.SmartList;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author DASK
 * @date 2020/3/30 1:49
 * @description //TODO 设计模式抽象基类
 */
public abstract class DesignPattern {
    protected AnActionEvent event;
    protected GoFile file;
    protected Project project;
    protected Editor editor;

    public DesignPattern(@NotNull AnActionEvent event) {
        this.event = event;
        this.file = (GoFile) event.getData(LangDataKeys.PSI_FILE);
        this.project = event.getProject();
        this.editor = event.getData(LangDataKeys.EDITOR);
    }

    //创建模板内容
    protected abstract void createTemplate(@NotNull GoTemplate template);

    protected abstract GoTypeSpec getGoTypeSpec();

    //获取当前文件中所有方法和结构体使用到的引用
    public static Set<GoRefactoringUtil.Import> getImports(@NotNull GoFile file, @NotNull Editor editor, @Nullable GoTypeSpec typeSpecToGenerate) {
        final Set<GoRefactoringUtil.Import> importsToAdd = new HashSet<>();
        //获取已经存在的方法
        List<GoNamedSignatureOwner> existingMethods = typeSpecToGenerate != null ? typeSpecToGenerate.getAllMethods() : ContainerUtil.emptyList();

        Iterator iterator;
        if (typeSpecToGenerate != null) {
            iterator = typeSpecToGenerate.getAllMethods().iterator();
            while (iterator.hasNext()) {
                GoNamedSignatureOwner m = (GoNamedSignatureOwner) iterator.next();
                if (!isAlreadyImplemented(m, existingMethods)) {
                    m.accept(new PsiRecursiveElementWalkingVisitor() {
                        public void visitElement(PsiElement o) {
                            if (o instanceof GoTypeReferenceExpression) {
                                GoType resolveType = ((GoTypeReferenceExpression) o).resolveType(GoPsiImplUtil.createContextOnElement(file));
                                GoRefactoringUtil.TypeTextWithImports typeTextWithImports = GoRefactoringUtil.getTypeTextWithImports(file, resolveType, false);
                                importsToAdd.addAll(typeTextWithImports.imports);
                            } else {
                                super.visitElement(o);
                            }
                        }
                    });
                }
            }
            return importsToAdd;
        }
        return importsToAdd;
    }

    //产生函数参数结构，如:(a int, b int)
    protected static String setupFunctionParameters(@NotNull List<FieldInfo> fields) {
        StringBuilder builder = new StringBuilder();
        builder.append("(");

        for (int i = 0; i < fields.size(); ++i) {
            FieldInfo field = fields.get(i);
            builder.append(StringUtil.decapitalize(field.getName()));
            builder.append(" ").append(field.getType());
            if (i != fields.size() - 1) {
                builder.append(", ");
            }
        }

        builder.append(")");
        return builder.toString();
    }

    //产生结构体内部成员内容，如{a: a, b: b}
    protected static String setupStructLiteralArguments(@NotNull List<FieldInfo> fields) {
        StringBuilder builder = new StringBuilder();
        builder.append("{");

        for (int i = 0; i < fields.size(); ++i) {
            FieldInfo field = fields.get(i);
            builder.append(field.getName()).append(": ");
            builder.append(field.getName());
            if (i != fields.size() - 1) {
                builder.append(", ");
            }
        }

        builder.append("}");
        return builder.toString();
    }

    //获取结构体成员变量
    protected static List<FieldInfo> getStructFields(GoTypeSpec typeSpec) {
        List<FieldInfo> fields = new SmartList();
        if (!typeSpec.isValid()) return fields;

        //获取声明的结构体
        GoTypeDeclaration typeDeclaration = PsiTreeUtil.getParentOfType(typeSpec, GoTypeDeclaration.class);
        //判断获取的结构体声明是否有效
        if (typeDeclaration != null && typeDeclaration.isValid()) {
            //获取实际定义的结构体
            GoStructType structType = (GoStructType) typeSpec.getSpecType().getType();

            if (structType != null) {
                //获取对应成员变量
                List<GoNamedElement> definitions = ContainerUtil.filter(structType.getFieldDefinitions(), (fd) -> !fd.isBlank());
                //遍历获取变量列表
                for (GoNamedElement namedElement : definitions) {
                    if (GoPsiImplUtil.isFieldDefinition(namedElement) && !namedElement.isBlank()) {
                        String name = namedElement.getName();
                        if (StringUtil.isEmptyOrSpaces(name)) {
                            return fields;
                        }
                        GoType type = namedElement.getGoType(null);
                        if (type != null) {
                            fields.add(new FieldInfo(name, type.getText()));
                        }
                    }
                }
            }
        }
        return fields;
    }

    protected int getCalcOffset(Editor editor, GoTypeSpec typeSpec) {
        GoTypeDeclaration typeDeclaration = PsiTreeUtil.getParentOfType(typeSpec, GoTypeDeclaration.class);
        return getCalcOffset(editor, typeDeclaration);
    }

    //获取当前选择对象文本块的偏移值
    protected int getCalcOffset(@NotNull Editor editor, @Nullable GoTypeDeclaration o) {
        if (o == null) {
            return editor.getCaretModel().getOffset();
        } else {
            PsiElement next = PsiTreeUtil.nextVisibleLeaf(o);
            return next instanceof LeafPsiElement && ((LeafPsiElement) next).getElementType() == GoTypes.SEMICOLON ? next.getTextRange().getEndOffset() : o.getTextRange().getEndOffset();
        }
    }

    public void generateText() {
        GoTemplate template = new GoTemplate(file);
        createTemplate(template);
        Set<GoRefactoringUtil.Import> set = new HashSet<>();
        for (GoTypeSpec spec : file.getTypes()) {
            if (!GoTypeUtil.isInterface(spec)) set.addAll(getImports(file, editor, spec));
        }
        template.startTemplate(editor, getCalcOffset(editor, getGoTypeSpec()), "Generate " + this.getClass().getSimpleName(), null, set);
        reformatCode();
        optimizeImports();
    }

    //判断当前结构体是否有了其他方法。如果有，那么其他方法中的接受者的名称将被用来生成新方法的接受者名称
    protected static boolean hasNamedReceivers(@NotNull GoTypeSpec typeSpecToGenerate) {
        List<GoNamedSignatureOwner> existingMethods = typeSpecToGenerate.getAllMethods();
        return existingMethods.stream().anyMatch((method) -> {
            GoReceiver receiver = method instanceof GoMethodDeclaration ? ((GoMethodDeclaration) method).getReceiver() : null;
            return receiver != null && StringUtil.isNotEmpty(receiver.getName());
        });
    }

    //判断当前结构体是否已经实现对应接口
    private static boolean isAlreadyImplemented(@NotNull GoNamedSignatureOwner m, @NotNull List<GoNamedSignatureOwner> existingMethod) {
        return existingMethod.stream().anyMatch((em) -> Comparing.equal(em.getName(), m.getName()) && GoTypeUtil.isSignaturesIdentical(m.getSignature(), em.getSignature(), true));
    }

    //检查是否用重复方法
    protected boolean checkDuplicateMethod(String method,String receiver){
        AtomicBoolean isDuplicated= new AtomicBoolean(false);
        for (GoMethodDeclaration declaration : file.getMethods()) {
            /*方法名*/
            if (receiver==null){
                if (method.equals(declaration.getName())) {
                    isDuplicated.set(true);
                    break;
                }
            }else {
                if (method.equals(declaration.getName())){
                    Optional.ofNullable(declaration.getReceiverType())
                            .map(PsiElement::getText)
                            .ifPresent((r)-> {
                                if (receiver.equals(r)){
                                    isDuplicated.set(true);
                                }
                    });
                    if (isDuplicated.get()) break;
                }
            }
        }
        return isDuplicated.get();
    }

    //检查是否有重复结构体或结构体
    protected boolean checkDuplicateStructOrInterface(String name){
        AtomicBoolean isDuplicated= new AtomicBoolean(false);
        for (GoTypeSpec  goTypeSpec : file.getTypes()) {
            if (name.equals(goTypeSpec.getName())) {
                isDuplicated.set(true);
                break;
            }
        }
        return isDuplicated.get();
    }

    //优化引用包的组织
    protected void optimizeImports() {
        //使用官方的自动导包方法
        OptimizeImportsAction.actionPerformedImpl(event.getDataContext());
    }

    //代码格式化
    protected void reformatCode() {
        new ReformatCodeAction().actionPerformed(event);
    }

    //向模板中添加换行
    protected void templateNewLine(GoTemplate template, int num) {
        for (int i = 0; i < num; i++) {
            template.addTextSegment("\n");
        }
    }

    //插入缩进
    protected void templateAddIndentation(GoTemplate template, int num) {
        for (int i = 0; i < num; i++) {
            template.addTextSegment("\t");
        }
    }

    public static class FieldInfo {
        private final String name;
        private final String type;

        public FieldInfo(@NotNull String name, @NotNull String type) {
            this.name = name;
            this.type = type;
        }

        public String getName() {
            return name;
        }

        public String getType() {
            return type;
        }
    }

    public static class MethodInfo {
        private String name,result;
        private List<FieldInfo> parameters;
        private List<FieldInfo> results;

        private MethodInfo() {
        }

        public static List<MethodInfo> Parse(GoTypeSpec interfaceToImpl) {
            if (!interfaceToImpl.isValid()) return null;

            List<MethodInfo> list = new ArrayList<>();
            for (GoNamedSignatureOwner signatureOwner : interfaceToImpl.getAllMethods()) {
                MethodInfo info = new MethodInfo();
                info.setName(signatureOwner.getName());
                GoSignature signature = signatureOwner.getSignature();
                Optional.ofNullable(signature)
                        .map(GoSignature::getParameters)
                        .ifPresent(goParameters -> {
                            List<FieldInfo> fieldInfos = new ArrayList<>();
                            for (GoParamDefinition definition : goParameters.getDefinitionList()) {
                                fieldInfos.add(new FieldInfo(definition.getName(), definition.getGoType(null).getText()));
                            }
                            info.setParameters(fieldInfos);
                        });
                Optional.ofNullable(signature)
                        .map(GoSignature::getResult)
                        .ifPresent((result) -> {
                            List<FieldInfo> fieldInfos=new ArrayList<>();
                            if (!result.isVoid()) {
                                Optional.ofNullable(result.getParameters())
                                        .ifPresent(p -> {
                                            List<FieldInfo> infos=new ArrayList<>();
                                            for (GoParamDefinition definition : p.getDefinitionList()) {
                                                infos.add(new FieldInfo(
                                                        Optional.ofNullable(definition.getName()).orElse(""),
                                                        Optional.ofNullable(definition.getGoType(null))
                                                        .map(PsiElement::getText).orElse("")
                                                ));
                                            }
                                            info.setResults(infos);
                                        });
                                //为空说明是单参数
                                if (info.getResults()==null||info.getResults().isEmpty()){
                                    info.setResult(signature.getResultType().getText());
                                }
                            }else{
                                info.setResult("");
                            }
                            info.setResults(fieldInfos);
                });
                list.add(info);
            }
            return list;
        }

        public String getParametersText(){
            if (parameters==null|| parameters.isEmpty()) return "()";
            return setupFunctionParameters(parameters);
        }

        public String getResultsText(){
            if (result!=null){
                return result;
            }
            if (results==null){
                return "";
            }
            return setupFunctionParameters(results);
        }

        public String getParametersTextNoType(){
            StringBuilder builder=new StringBuilder();
            if (parameters==null|| parameters.isEmpty()) return "()";
            builder.append("(");
            for (int i = 0; i < parameters.size(); i++) {
                String fieldName=parameters.get(i).getName();
                if (i==parameters.size()-1){
                    builder.append(fieldName);
                }else {
                    builder.append(fieldName).append(",");
                }
            }
            builder.append(")");
            return builder.toString();
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public List<FieldInfo> getParameters() {
            return parameters;
        }

        public void setParameters(List<FieldInfo> parameters) {
            this.parameters = parameters;
        }

        public List<FieldInfo> getResults() {
            return results;
        }

        public void setResults(List<FieldInfo> results) {
            this.results = results;
        }

        public String getResult() {
            return result;
        }

        public void setResult(String result) {
            this.result = result;
        }
    }
}
