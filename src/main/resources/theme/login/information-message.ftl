<#import "template.ftl" as layout>
<@layout.registrationLayout section="form">
    <form id="kc-info-message-form" action="${url.loginAction}" method="post">
        <div class="kc-form-group">
            <div class="kc-logo-text">
                <h3>${msg("informationMessageTitle", "Important Information")}</h3>
            </div>
        </div>

        <div class="kc-info-message">
            <p>${informationMessage}</p>
        </div>

        <div id="kc-form-buttons" class="kc-form-buttons">
            <input class="${properties.kcButtonClass!} ${properties.kcButtonPrimaryClass!} ${properties.kcButtonLargeClass!}" name="ok" id="kc-ok" type="submit" value="${msg("ok", "OK")}"/>
        </div>
    </form>
</@layout.registrationLayout>
