<%@ page contentType="text/html;charset=UTF-8" %>
<html>
<head>
    <title>Issue EFID</title>
    <meta name='layout' content='main'/>

    <g:javascript>

        $(function() {
            var clearStatus = function() {
               $('#verifyStatus').text("");
            };
            $('#verifyEfid').change(clearStatus).focus(clearStatus);
            $('#verifyEfid').focus(function(){$(this).val("");});
        });

    </g:javascript>

</head>
<body>

<h3>Verify an EFID</h3>

<g:formRemote name="verifyEfidForm" url="[action:'verify']" autocomplete='off'
        before="if (\$('#verifyEfid').val() == '') return false;"
        onSuccess="\$('#verifyStatus').text('Valid!');"
        onFailure="\$('#verifyStatus').text( XMLHttpRequest.status == 404 ? 'Invalid!' : textStatus);">
    <input name="id" id="verifyEfid" type="text" maxlength="11" />
    <input type="submit" value="Verify"/>
    <div id="verifyStatus"></div>
</g:formRemote>

<br/>

<sec:ifAllGranted roles="ROLE_CREATE">
    <g:javascript>
        function updateAlottment() {
            $('#alottedIds').text("");
            $.ajax({
                type:"GET",
                url: "${createLink(controller:'efidIssuer', action:'mydata')}",
                dataType: "json",
                success: function(data) {
                    $('#alottedIds').text("Alotted EFIDs: "+data.efids+" / "+data.maxEfids);
                }
            });
        }

        $(function() {
            updateAlottment();
        });
    </g:javascript>

    <h3>Issue an EFID</h3>
    <g:formRemote name="issueEfidForm" url="[action:'issue']" autocomplete='off'
            onSuccess="\$('#issueStatus').text(data);"
            onFailure="\$('#issueStatus').text( XMLHttpRequest.status == 403 ? 'Cannot issue more.' : 'Error '+errorThrown );"
            after="updateAlottment();">
        <g:submitButton name="submit" value="Issue" id="issueSubmit"/>
        <div id="issueStatus"></div>
    </g:formRemote>


    <br />
    <span id="alottedIds"></span>

</sec:ifAllGranted>

</body>
</html>