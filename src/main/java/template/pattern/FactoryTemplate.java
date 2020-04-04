package template.pattern;

import com.goide.intentions.GoImplementInterfaceIntention;
import com.goide.psi.GoFile;
import com.goide.psi.GoType;
import com.goide.psi.GoTypeSpec;
import com.goide.refactor.GoImplementMethodsHandler;
import com.goide.refactor.template.GoTemplate;
import com.intellij.codeInsight.template.impl.ConstantNode;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.util.text.StringUtil;
import jdk.internal.dynalink.linker.LinkerServices;
import org.jetbrains.annotations.NotNull;
import template.DesignPattern;
import utils.PopupUtil;

import java.util.List;

/**
 * @author DASK
 * @date 2020/3/30 19:49
 * @description //TODO
 */
public class FactoryTemplate extends DesignPattern {
    private List<GoTypeSpec> lists;//要实现工厂模式的结构体
    private GoTypeSpec interfaceToImpl;//工厂模式返回的接口

    public FactoryTemplate(@NotNull AnActionEvent event, List<GoTypeSpec> lists, GoTypeSpec interfaceToImpl) {
        super(event);
        this.lists = lists;
        this.interfaceToImpl = interfaceToImpl;
    }

    @Override
    protected void createTemplate(@NotNull GoTemplate template) {
        for (GoTypeSpec structType:lists){
            PopupUtil.createUnImplementMethod(file,editor,structType,interfaceToImpl);
        }
        createFactoryStruct(template);
    }

    private void createFactoryStruct(GoTemplate template) {
        String interfaceName=StringUtil.capitalize(interfaceToImpl.getName());


        template.addTextSegment("\n\ntype "+interfaceName+"Factory struct{}\n");

        /*func (shapeFactory *ShapeFactory) getShape(shapeName string) Shape {
            if shapeName == "" {
                return nil
            }
            switch strings.ToLower(shapeName) {
                case "circle":
                    return new(Circle)
                case "square":
                    return new(Square)
                default:
                    return nil
            }
        }*/
        StringBuilder builder=new StringBuilder();

        String format="\nfunc (%sFactory *%sFactory) get%s(name string) %s {\n" +
                "\tif name == \"\" {\n" +
                "\t\treturn nil\n" +
                "\t}\n" +
                "\tswitch strings.ToLower(name) {\n";
        builder.append(String.format(format,
                StringUtil.decapitalize(interfaceName),interfaceName,interfaceName,interfaceToImpl.getName()
        ));

        String format1="\t\tcase \"%s\":\n" +
                "\t\t\treturn new(%s)\n";
        for (GoTypeSpec typeSpec:lists) {
            builder.append(String.format(format1,
                StringUtil.toLowerCase(typeSpec.getName()),typeSpec.getName()
            ));
        }

        builder.append("\t\tdefault:\n" +
                "\t\t\treturn nil\n" +
                "\t}\n" +
                "}");
        template.addTextSegment(builder.toString());
    }


    @Override
    protected GoTypeSpec getGoTypeSpec() {
        return interfaceToImpl;
    }
}
