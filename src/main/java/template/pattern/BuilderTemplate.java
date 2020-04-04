package template.pattern;

import com.goide.psi.GoTypeSpec;
import com.goide.refactor.template.GoTemplate;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.util.text.StringUtil;
import org.jetbrains.annotations.NotNull;
import template.DesignPattern;
import utils.PopupUtil;

import java.util.List;

/**
 * @author DASK
 * @date 2020/3/30 16:10
 * @description //TODO 建造者模式模板
 */
public class BuilderTemplate extends DesignPattern {
    private GoTypeSpec goTypeSpec;

    public BuilderTemplate(@NotNull AnActionEvent event,@NotNull GoTypeSpec goTypeSpec) {
        super(event);
        this.goTypeSpec = goTypeSpec;
    }

    @Override
    protected void createTemplate(@NotNull GoTemplate template) {
        List<FieldInfo> list= PopupUtil.getChooseFieldPopup(goTypeSpec,project,null);
        if (list.isEmpty()) {
            return;
        }
        String typeName=goTypeSpec.getName();
        createSetterAndGetter(template,typeName,list);
        createBuildInterface(template,typeName,list);
        createBuilderStruct(template,typeName);
        createInterfaceImpl(template,typeName,list);
    }

    //为Builder对象实现接口方法
    private void createInterfaceImpl(GoTemplate template, String typeName, List<FieldInfo> list) {
        StringBuilder builder=new StringBuilder();
        String builderName= StringUtil.capitalize(typeName)+"Builder";
        String lowerName=StringUtil.decapitalize(typeName);
        /*func NewFruitBuilder(fruit *Fruit) *FruitBuilder {
            return &FruitBuilder{fruit: fruit}
          }
        */
        String format="\nfunc New%s(%s *%s) *%s {\n" +
                "\treturn &%s{%s: %s}\n}\n";
        builder.append(String.format(format,
                builderName, lowerName, typeName, builderName,
                builderName, lowerName, lowerName
        ));
        /*func (f FruitBuilder) Create(name, color string) *Fruit {
            return f.SetName(name).SetColor(color).Build()
        }*/
        builder.append(String.format("\nfunc (builder %s) Create%s *%s{\n",
                builderName,setupFunctionParameters(list),typeName
        ));

        builder.append("\treturn builder");
        for (FieldInfo info:list){
            builder.append(String.format(".Set%s(%s)",
                    StringUtil.capitalize(info.getName()),
                    StringUtil.decapitalize(info.getName())
            ));
        }
        builder.append(".Build()\n}\n");
        /*func (f FruitBuilder) Build() *Fruit {
            return f.fruit
        }*/
        builder.append(String.format(
                "\nfunc (builder %s) Build() *%s {\n" +
                    "\treturn builder.%s" +
                "\n}",
                builderName,typeName,lowerName
        ));
        /*func (builder FruitBuilder) SetName(name string) Builder {
            if builder.fruit==nil {
                builder.fruit=&Fruit{}
            }
            builder.fruit.SetName(name)
            return builder
        }*/
        String format1="\nfunc (builder %s) Set%s(%s %s) Builder {\n" +
                "\tif builder.%s == nil {\n" +
                "\t\tbuilder.%s = &%s{}\n" +
                "\t}\n" +
                "\tbuilder.%s.Set%s(%s)\n" +
                "\treturn builder\n" +
                "}\n";
        for (FieldInfo info:list){
            builder.append(String.format(format1,
                    builderName,StringUtil.capitalize(info.getName()),
                    info.getName(),info.getType(),
                    lowerName,lowerName,typeName,lowerName,
                    StringUtil.capitalize(info.getName()),info.getName()
            ));
        }
        template.addTextSegment(builder.toString());
    }

    //生成Builder对象
    private void createBuilderStruct(GoTemplate template, String typeName) {
        String format="\ntype %sBuilder struct {\n" +
                "\t%s *%s\n" +
                "}\n";
        String text=String.format(format,
                StringUtil.capitalize(typeName),
                StringUtil.decapitalize(typeName),
                typeName
        );
        template.addTextSegment(text);
    }

    //生成build接口
    private void createBuildInterface(GoTemplate template, String typeName, List<FieldInfo> list) {
        StringBuilder builder=new StringBuilder();
        builder.append("type Builder interface {\n");
        for (FieldInfo info:list){
            builder.append("\tSet")
                    .append(StringUtil.capitalize(info.getName()))
                    .append("(")
                    .append(info.getName()).append(" ").append(info.getType())
                    .append(") Builder\n");
        }
        builder.append("\tBuild() *")
                .append(typeName)
                .append("\n")
                .append("\tCreate")
                .append(setupFunctionParameters(list))
                .append(" *").append(typeName).append("\n}");

        template.addTextSegment(builder.toString());
    }

    //为当前结构体生成Setter&Getter
    private void createSetterAndGetter(@NotNull GoTemplate template,String typeName,@NotNull List<FieldInfo> lists){
        StringBuilder builder=new StringBuilder();
        String firstLower=StringUtil.decapitalize(typeName);

        String templateFormat="\nfunc (%s *%s) Set%s(%s %s)  {\n" +
                "\t\t%s.%s=%s\n" +
                "}\n" +
                "\n" +
                "func (%s *%s) Get%s() %s {\n" +
                "\treturn %s.%s\n" +
                "}\n";

        for (FieldInfo info:lists){
            builder.append(String.format(templateFormat,
                    //Setter
                    firstLower, typeName,
                    StringUtil.capitalize(info.getName()),
                    info.getName(),info.getType(),
                    firstLower,
                    info.getName(),info.getName(),
                    //Getter
                    firstLower, typeName,
                    StringUtil.capitalize(info.getName()),
                    info.getType(),
                    firstLower,info.getName()
                    ));
        }
        template.addTextSegment(builder.toString());
    }

    @Override
    protected GoTypeSpec getGoTypeSpec() {
        return goTypeSpec;
    }
}
