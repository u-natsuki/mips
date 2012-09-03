// place an icon where you like

def icon = "${System.getenv()["HOME"]}/svnconfig/notification_done.png"
def root = new File("./src/")
println root.absolutePath
lastmodified = [:]
current = [:]
while(true){
    current.clear()
    root.eachFileRecurse {
        current << ["${it.absolutePath}":it.lastModified()]
    }
    if(!current.equals(lastmodified)){
        println "build"
        def p = "gradle test".execute()
        p.inputStream.eachLine("UTF-8"){
            println it
        }
        int i = p.waitFor()
        if(i == 0){
            "notify-send --icon=${icon}  -u normal Succeeded".execute()
            green = "git commit -am build_success".execute()
            green.inputStream.eachLine("UTF-8"){
                println it
            }
            green.errorStream.eachLine("UTF-8"){
                println it
            }
            greeni = green.waitFor()
        }
        else{
            "notify-send --icon=${icon}  -u critical Failed".execute()
            red = "git commit -am build_fail".execute()
            red.inputStream.eachLine("UTF-8"){
                println it
            }
            red.errorStream.eachLine("UTF-8"){
                println it
            }
            redi = red.waitFor()
        }
        lastmodified.putAll(current)
    }
    sleep(1000)
}
