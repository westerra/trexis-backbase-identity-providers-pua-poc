<#import "template.ftl" as layout>
<@layout.registrationLayout; section>
    <#if section = "header">
        ${msg("doLogIn")}
    <#elseif section = "form">
        <form id="kc-totp-login-form" class="${properties.kcFormClass!}" action="${url.loginAction}" method="post">
            <div class="${properties.kcFormGroupClass!}">
                <div class="${properties.kcLabelWrapperClass!}">
                    <h3 class="${properties.kcLabelClass!}">${msg("MFASecondTitle")}</h3>
                </div>
            </div>
            <div class="form-group">
                <span>
                    ${msg("MFAChooseMethod")}
                </span>
            </div>
            <div>
                <div class="form-group">
                    <#if otpChoiceList??>
                        <#list otpChoiceList>
                            <#items as otpChoice>
                                <div>
                                    <input id="${otpChoice.channel}" name="mfa.selector.choice.addressId" value="${otpChoice.addressId}" type="radio" ${otpChoice.selected?then('checked', '')}>
                                    <label for="${otpChoice.channel}">${msg(otpChoice.channel)} ${otpChoice.address}</label>
                                </div>
                            </#items>
                        </#list>
                    </#if>
                </div>
            </div>

            <div class="${properties.kcFormGroupClass!}">
                <div id="kc-form-options" class="${properties.kcFormOptionsClass!}">
                    <div class="${properties.kcFormOptionsWrapperClass!}">
                    </div>
                </div>

                <div id="kc-form-buttons" class="${properties.kcFormButtonsClass!}">
                    <div style="display:flex" class="${properties.kcFormButtonsWrapperClass!}">
                        <input class="${properties.kcButtonClass!} ${properties.kcButtonPrimaryClass!} ${properties.kcButtonLargeClass!}" name="login" id="kc-login" type="submit" value="${msg("goNext")}"/>
                        <input style="order:-1" class="${properties.kcButtonClass!} ${properties.kcButtonDefaultClass!} ${properties.kcButtonLargeClass!}" name="cancel" id="kc-cancel" type="submit" value="${msg("goBack")}"/>
                    </div>
                </div>
            </div>
        </form>
    </#if>
</@layout.registrationLayout>