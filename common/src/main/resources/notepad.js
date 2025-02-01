export default function(application) {
  var window = application.createWindow({
    title: "Notepad",
    width: 200,
    height: 300,
    center: true,
    resizable: false,
    minimizable: false,
    maximizable: false,
  })

  window.add(new Button(10, 10, 100, 20, "Hello World!"))
  window.show()
}