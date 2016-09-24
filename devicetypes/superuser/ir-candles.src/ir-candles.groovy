/**
*	IR Candle Controller (using Raspberry Pi 3, LIRC and LIRC_Web)
*  Credit goes to Patrick Stuart for the hubAction code.
*  
*  Copyright 2015 blebson
*
*  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
*  in compliance with the License. You may obtain a copy of the License at:
*
*      http://www.apache.org/licenses/LICENSE-2.0
*
*  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
*  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
*  for the specific language governing permissions and limitations under the License.
*
*/
metadata {
    definition (name: "IR Candles", author: "blebson") {
        capability "Switch"

        attribute "hubactionMode", "string"
        attribute "switch2", "string"
        attribute "switch3", "string"
        attribute "switch4", "string"
        attribute "switch5", "string"
        attribute "switch6", "string"
        attribute "switch7", "string"
        attribute "switch8", "string"
        attribute "switch9", "string"

        //command
        command "brup"
        command "brdown"
        command "twoH"
        command "fourH"
        command "sixH"
        command "eightH"
        command "candle"
        command "light"

    }

    preferences {
        input("IP", "string", title:"RPI IP Address", description: "Please enter your Raspberry PI's IP Address", required: true, displayDuringSetup: true)
        input("Port", "string", title:"RPI Port", description: "Please enter your Raspberry PI's HTTP Port", defaultValue: 3000 , required: true, displayDuringSetup: true)    
    }
    simulator {

    }

    tiles (scale: 2){

        standardTile("OnOff", "device.switch", width: 4, height: 4, canChangeIcon: false) {
            state "off", label: 'Off', action: "switch.on", icon: "st.lights.multi-light-bulb-off", backgroundColor: "#ffffff", nextState: "on"
            state "toggle", label:'toggle', action: "", icon: "st.lights.light-bulb-on", backgroundColor: "#53a7c0"
            state "on", label: 'On', action: "switch.off", icon: "st.lights.multi-light-bulb-on", backgroundColor: "#66ff66", nextState: "off"     
        }
        standardTile("up", "device.switch2", width: 2, height: 2, canChangeIcon: false) {
            state "up", label: "", action: "brup", icon: "st.thermostat.thermostat-up", backgroundColor: "#ffff66"
        }
        standardTile("down", "device.switch3", width: 2, height: 2, canChangeIcon: false) {
            state "down", label: "", action: "brdown", icon: "st.thermostat.thermostat-down", backgroundColor: "#6699ff"
        }
        valueTile("twoH", "device.switch4", width: 2, height: 2, canChangeIcon: false) {
            state "twoH", label: "2 Hour", action: "twoH"
        }
        valueTile("fourH", "device.switch5", width: 2, height: 2, canChangeIcon: false) {
            state "fourH", label: "4 Hour", action: "fourH"
        }
        valueTile("sixH", "device.switch6", width: 2, height: 2, canChangeIcon: false) {
            state "sixH", label: "6 Hour", action: "sixH"
        }
        valueTile("eightH", "device.switch7", width: 2, height: 2, canChangeIcon: false) {
            state "eightH", label: "8 Hour", action: "eightH"
        }
        standardTile("candle", "device.switch8", width: 2, height: 2, canChangeIcon: false) {
            state "candle", label: "Candle Mode", action: "candle", icon: "st.Seasonal Winter.seasonal-winter-018"
        }
        standardTile("light", "device.switch9", width: 2, height: 2, canChangeIcon: false) {
            state "light", label: "Light Mode", action: "light", icon: "st.Lighting.light11"
        }

        main "OnOff"
        details(["OnOff", "up", "down", "twoH", "fourH", "candle", "sixH", "eightH", "light"])
    }
}

def parse(String description) {
    log.debug "Parsing '${description}'"
    def map = [:]
    def retResult = []
    def descMap = parseDescriptionAsMap(description)
    def msg = parseLanMessage(description)
    //log.debug "status ${msg.status}"
    //log.debug "data ${msg.data}"

    def aheader = new String(descMap["headers"].decodeBase64())
    log.debug "Header: ${aheader}"

    device.deviceNetworkId = "ID_WILL_BE_CHANGED_AT_RUNTIME_" + (Math.abs(new Random().nextInt()) % 99999 + 1)
}

// handle commands
def irSend(String attr)
{

    def host = IP 
    def hosthex = convertIPtoHex(host)
    def porthex = convertPortToHex(Port)
    device.deviceNetworkId = "$hosthex:$porthex" 

    log.debug "The device id configured is: $device.deviceNetworkId"

    def headers = [:] 
    headers.put("HOST", "$host:$Port")

    log.debug "The Header is $headers"


    def path = "/remotes/candles/${attr}"
    log.debug "path is: $path"
    try {
        def hubAction = new physicalgraph.device.HubAction(
            method: "POST",
            path: path,
            headers: headers
        )


        log.debug hubAction
        return hubAction

    }
    catch (Exception e) {
        log.debug "Hit Exception $e on $hubAction"
    }
}

def parseDescriptionAsMap(description) {
    description.split(",").inject([:]) { map, param ->
        def nameAndValue = param.split(":")
        map += [(nameAndValue[0].trim()):nameAndValue[1].trim()]
    }
}

private String convertIPtoHex(ipAddress) { 
    String hex = ipAddress.tokenize( '.' ).collect {  String.format( '%02x', it.toInteger() ) }.join()
    log.debug "IP address entered is $ipAddress and the converted hex code is $hex"
    return hex

}

private String convertPortToHex(port) {
    String hexport = port.toString().format( '%04x', port.toInteger() )
    log.debug hexport
    return hexport
}

private Integer convertHexToInt(hex) {
    Integer.parseInt(hex,16)
}


private String convertHexToIP(hex) {
    log.debug("Convert hex to ip: $hex") 
    [convertHexToInt(hex[0..1]),convertHexToInt(hex[2..3]),convertHexToInt(hex[4..5]),convertHexToInt(hex[6..7])].join(".")
}

private getHostAddress() {
    def parts = device.deviceNetworkId.split(":")
    log.debug device.deviceNetworkId
    def ip = convertHexToIP(parts[0])
    def port = convertHexToInt(parts[1])
    return ip + ":" + port
}

def on() {
    delayBetween([
        onButton(),
        onButton(),
        onButton()
    ], 2000)
}

def onButton() {
    log.debug "On"
    sendEvent(name: "switch", value: "on")
    return irSend("key_power")
}

def off() {
    delayBetween([
        offButton(),
        offButton(),
        offButton()
    ], 2000)
}

def offButton() {
    log.debug "Off"    
    sendEvent(name: "switch", value: "off")
    return irSend("key_power2")
}

def twoH() {
    delayBetween([
        twoHButton(),
        twoHButton(),
        twoHButton()
    ], 2000)
}

def twoHButton() {
    log.debug "2 Hour Timer"
    return irSend("key_2")
}

def fourH() {
    delayBetween([
        fourHButton(),
        fourHButton(),
        fourHButton()
    ], 2000)
}

def fourHButton() {
    log.debug "4 Hour Timer"
    return irSend("key_4")
}

def sixH() {
    delayBetween([
        sixHButton(),
        sixHButton(),
        sixHButton()
    ], 2000)
}

def sixHButton() {
    log.debug "6 Hour Timer"
    return irSend("key_6")
}

def eightH() {
    delayBetween([
        eightHButton(),
        eightHButton(),
        eightHButton()
    ], 2000)
}

def eightHButton() {
    log.debug "8 Hour Timer"
    return irSend("key_8")
}

def brup() {
    delayBetween([
        upButton(),
        upButton(),
        upButton()
    ], 2000)
}

def upButton() {
    log.debug "Brightness Up"
    return irSend("key_brightnessup")    
}

def brdown() {
    delayBetween([
        downButton(),
        downButton(),
        downButton()
    ], 2000)
}

def downButton() {
    log.debug "Brightness Down"
    return irSend("key_brightnessdown")    
}

def candle() {
    delayBetween([
        candleButton(),
        candleButton(),
        candleButton()
    ], 2000)
}

def candleButton() {
    log.debug "Candle Mode"
    return irSend("key_prog1")    
}

def light() {
    delayBetween([
        lightButton(),
        lightButton(),
        lightButton()
    ], 2000)
}

def lightButton() {
    log.debug "Light Mode"    
    return irSend("key_prog2")    
}
