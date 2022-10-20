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
                    <div class="container__choices">
                        <#if (otpChoiceByChannel??)>
                            <#list otpChoiceByChannel?keys as key>
                                <div class="category__container">
                                    <div class="category__icon">
                                        <#--  there must be a better way to do this but need to ask around  -->
                                        <#switch key>
                                            <#case "text">
                                                <svg version="1.1" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 768 768">
                                                    <title>${key}</title>
                                                    <g id="icomoon-ignore">
                                                    </g>
                                                    <path fill="#000" d="M544.5 607.5v-447h-321v447h321zM544.5 33q25.5 0 44.25 18.75t18.75 44.25v576q0 25.5-18.75 45t-44.25 19.5h-321q-25.5 0-44.25-19.5t-18.75-45v-576q0-25.5 18.75-45t44.25-19.5z"></path>
                                                </svg>
                                                <#break>
                                            <#case "voice">
                                                <svg version="1.1" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 768 768">
                                                    <title>${key}</title>
                                                    <g id="icomoon-ignore">
                                                    </g>
                                                    <path fill="#000" d="M211.5 345q72 139.5 211.5 211.5l70.5-70.5q15-15 33-7.5 54 18 114 18 13.5 0 22.5 9t9 22.5v112.5q0 13.5-9 22.5t-22.5 9q-225 0-384.75-159.75t-159.75-384.75q0-13.5 9-22.5t22.5-9h112.5q13.5 0 22.5 9t9 22.5q0 60 18 114 6 19.5-7.5 33z"></path>
                                                </svg>
                                                <#break>
                                            <#default>
                                                <svg version="1.1" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 448 448">
                                                    <title>${key}</title>
                                                    <g id="icomoon-ignore">
                                                    </g>
                                                    <path fill="#000" d="M416 376v-192c-5.25 6-11 11.5-17.25 16.5-35.75 27.5-71.75 55.5-106.5 84.5-18.75 15.75-42 35-68 35h-0.5c-26 0-49.25-19.25-68-35-34.75-29-70.75-57-106.5-84.5-6.25-5-12-10.5-17.25-16.5v192c0 4.25 3.75 8 8 8h368c4.25 0 8-3.75 8-8zM416 113.25c0-6.25 1.5-17.25-8-17.25h-368c-4.25 0-8 3.75-8 8 0 28.5 14.25 53.25 36.75 71 33.5 26.25 67 52.75 100.25 79.25 13.25 10.75 37.25 33.75 54.75 33.75h0.5c17.5 0 41.5-23 54.75-33.75 33.25-26.5 66.75-53 100.25-79.25 16.25-12.75 36.75-40.5 36.75-61.75zM448 104v272c0 22-18 40-40 40h-368c-22 0-40-18-40-40v-272c0-22 18-40 40-40h368c22 0 40 18 40 40z"></path>
                                                </svg>
                                        </#switch>
                                    </div>
                                    <#assign msgKey = key+"Option">
                                    <div class="center">${msg(msgKey)}</div>
                                </div>
                                <div class="category__details">
                                    <#list otpChoiceByChannel[key] as otpChoice>
                                            <div class="category__item">
                                                <input type="radio" name="mfa.selector.choice.addressId" value="${otpChoice.addressId}" id="${otpChoice.channel}" value="${otpChoice.addressId}" ${otpChoice.selected?then('checked', '')} onclick="enableNext()"/>
                                                <label for="${otpChoice.channel}">${otpChoice.address}</label>
                                            </div>
                                    </#list>
                                </div>
                            </#list>
                        </#if>
                    </div>
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