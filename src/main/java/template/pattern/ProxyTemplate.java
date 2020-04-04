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
 * @date 2020/4/4 13:52
 * @description //TODO 代理模式模板
 */
public class ProxyTemplate extends DesignPattern {
    private GoTypeSpec structType;
    private GoTypeSpec interfaceType;

    public ProxyTemplate(@NotNull AnActionEvent event,@NotNull GoTypeSpec structType,@NotNull GoTypeSpec interfaceType) {
        super(event);
        this.structType = structType;
        this.interfaceType = interfaceType;
    }

    @Override
    protected void createTemplate(@NotNull GoTemplate template) {
        String structName=structType.getName();
        String interName=interfaceType.getName();
        //实现其中结构体的中未实现的接口方法
        PopupUtil.createUnImplementMethod(file,editor,structType,interfaceType);
        createProxyStruct(template,structName,interName);
        createProxyMethod(template,structType,interfaceType);
    }

    private void createProxyMethod(@NotNull GoTemplate template, @NotNull GoTypeSpec structType, @NotNull GoTypeSpec interfaceType) {
        StringBuilder builder=new StringBuilder();
        String proxyerName="Proxy"+StringUtil.capitalize(structType.getName());
        /*func (s ProxyServer) ProxyFor(method *ServerMethod)  {
            s.server=*method
        }*/
        String format="\nfunc (p %s) ProxyFor(%s *%s)  {\n" +
                "\tp.%s=*%s\n" +
                "}\n";
        builder.append(String.format(format,
                proxyerName,
                StringUtil.decapitalize(interfaceType.getName()),interfaceType.getName(),
                StringUtil.decapitalize(interfaceType.getName()),StringUtil.decapitalize(interfaceType.getName())
        ));
        List<MethodInfo> list=MethodInfo.Parse(interfaceType);
        String format1="\nfunc (p %s) %s%s %s{\n" +
                "\t/*do something before*/\n" +
                "\tp.%s.%s%s\n" +
                "\t/*do something after*/\n" +
                "}\n";
        for (MethodInfo info:list){
            builder.append(String.format(format1,
                    proxyerName,info.getName(),info.getParametersText(),info.getResultsText(),
                    StringUtil.decapitalize(interfaceType.getName()),info.getName(),info.getParametersTextNoType()
            ));
        }
        template.addTextSegment(builder.toString());
    }

    private void createProxyStruct(@NotNull GoTemplate template,@NotNull String structName,@NotNull String interName) {
        /*type ProxyServer struct {
            server ServerMethod
        }*/
        String format="\n\ntype Proxy%s struct {\n" +
                "\t%s %s\n" +
                "}\n";
        template.addTextSegment(String.format(format,
                StringUtil.capitalize(structName),
                StringUtil.decapitalize(interName), interName
        ));
    }


    @Override
    protected GoTypeSpec getGoTypeSpec() {
        return structType;
    }
}
