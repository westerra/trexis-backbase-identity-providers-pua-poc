<#import "template.ftl" as layout>
<@layout.registrationLayout; section>
  <#if section = "header">
    ${msg("doLogIn")}
  <#elseif section = "form">
    <script>
      var period = 60;
      var x = setInterval(function() {

        var distance = period --;
        document.getElementById("timeLeft").innerHTML = distance;

        if (distance < 0) {
          clearInterval(x);
          document.getElementById("timeLeft").innerHTML = "0";
        }
      }, 1000);
    </script>
   
    <form id="kc-totp-login-form" class="${properties.kcFormClass!}" action="${url.loginAction}" method="post">
        <div class="${properties.kcFormGroupClass!}">
          <div class="${properties.kcLabelWrapperClass!}">
            <h3 class="${properties.kcLabelClass!}">${msg("MFAThirdTitle")}</h3>
          </div>
        </div>
        <div class="test">
                  <span>
                      ${msg("MFACodeSent")}
                  </span>
        </div>
        
        <div class="code-input form-group">
            <label for="codeInput">${msg("enterCode")}</label>
            <input id="totp" name="totp" type="text" class="${properties.kcInputClass!}" placeholder="${msg("enterCode")}" type="text" autofocus autocomplete="off" />
        </div>
  
        <div style="display:flex" >
  
            <div class="${properties.kcFormGroupClass!}">
                <div id="form-group" class="${properties.kcFormOptionsClass!}">
                    <span class="expireText">${msg("codeExpire1")} <span id="timeLeft"></span> ${msg("codeExpire2")}</span>
                </div>
            
  
                <div style="display:flex" id="kc-form-buttons" class="${properties.kcFormButtonsClass!}">
                    <div class="${properties.kcFormButtonsWrapperClass!}">
                    <input class="${properties.kcButtonClass!} ${properties.kcButtonPrimaryClass!} ${properties.kcButtonLargeClass!}" name="login" id="kc-login" type="submit" value="${msg("goNext")}"/>
                    </div>
                    <#--  inputs reversed in DOM due to behavior of enter key submitting first input by default -->
                    <input style="order: -1" class="${properties.kcButtonClass!} ${properties.kcButtonDefaultClass!} ${properties.kcButtonLargeClass!}" name="cancel" id="kc-cancel" type="submit" value="${msg("goBack")}"/>
                </div>
            </div>

            <div style="order: -1;" class="${properties.kcFormOptionsWrapperClass!} resend-label">
                <span>
                <input class="${properties.kcButtonClass!} ${properties.kcButtonDefaultClass!} ${properties.kcButtonLargeClass!}" name="resend" id="kc-resend" type="submit" value="${msg("buttonResend")}"/>
                </span>
            </div>
        </div>
      </form>
  </#if>
</@layout.registrationLayout>