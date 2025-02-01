package dev.ultreon.devicesnext;

import dev.ultreon.devicesnext.tests.VDisk;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Virtual Disk")
class VDiskTest {
//    @Test
//    @DisplayName("Empty disk")
//    void emptyDisk() throws IOException {
//        VDisk disk = VDisk.format(Paths.get("test.vdisk"), 16 * 1024 * 1024);
//        assertEquals(16 * 1024 * 1024, disk.size());
//
//        disk.close();
//    }
//
//    @Test
//    @DisplayName("Read/Write data")
//    void readWriteData() throws IOException {
//        VDisk disk = VDisk.format(Paths.get("test.vdisk"), 16 * 1024 * 1024);
//        assertEquals(16 * 1024 * 1024, disk.size());
//
//        disk.createFile("test", new byte[1024]);
//        byte[] data = disk.readFile("test");
//
//        assertArrayEquals(new byte[1024], data);
//
//        disk.close();
//
//        VDisk disk2 = VDisk.open(Paths.get("test.vdisk"));
//        data = disk2.readFile("test");
//
//        assertArrayEquals(new byte[1024], data);
//
//        disk2.close();
//    }
//
//    @Test
//    @DisplayName("Read/Write Lorem Ipsum")
//    void readWriteLoremIpsum() throws IOException {
//        VDisk disk = VDisk.format(Paths.get("test.vdisk"), 16 * 1024 * 1024);
//        assertEquals(16 * 1024 * 1024, disk.size());
//
//        final String loremIpsum = """
//                Lorem ipsum dolor sit amet, consectetur adipiscing elit. Quisque sit amet vulputate dolor. Vivamus in nisl eget risus semper efficitur. Nunc et mollis nulla. Sed eleifend augue sed placerat imperdiet. Cras feugiat leo dolor, ac molestie massa luctus ac. Sed commodo quis justo at tempor. Nam at magna in nibh elementum ultrices a sit amet mauris. Fusce maximus turpis sit amet tellus laoreet, eget dapibus dui accumsan. Nulla pulvinar ultrices eros, id porta purus. Cras vel ultrices odio, eget feugiat odio. Maecenas sit amet fermentum ipsum. Fusce eget libero lacinia, gravida risus quis, vestibulum risus. In maximus mi nisi, at scelerisque metus tincidunt vel.
//
//                Mauris venenatis laoreet vulputate. Pellentesque habitant morbi tristique senectus et netus et malesuada fames ac turpis egestas. Phasellus tincidunt nulla et risus luctus, eu rutrum augue porttitor. Curabitur id tellus elit. Sed fermentum, felis mattis cursus convallis, ligula eros mollis lorem, non ullamcorper metus tellus in dolor. Nam a aliquam justo. Donec vel nibh nibh. Quisque vestibulum justo velit, nec facilisis nulla varius id. Maecenas risus libero, laoreet ut vestibulum vitae, ultricies quis eros. Praesent ut justo sem.
//
//                Nulla blandit condimentum pharetra. Nam ullamcorper nisi id aliquet interdum. Praesent vel purus lacinia, luctus ex vitae, sodales lorem. Duis ultrices consequat tincidunt. Quisque id mattis ante. Interdum et malesuada fames ac ante ipsum primis in faucibus. Sed venenatis vestibulum gravida. Maecenas neque nisl, tristique a gravida eget, pretium non ipsum. Nulla pharetra nibh justo.
//
//                Sed vel hendrerit mi. Proin vitae hendrerit nibh, a pellentesque est. Mauris venenatis vestibulum ex a feugiat. Etiam risus mauris, fringilla ut sem et, consectetur finibus est. Mauris faucibus a dui eget laoreet. Aliquam hendrerit sed erat at varius. Nulla nec condimentum sapien, ut efficitur tellus. Nam feugiat felis diam. Vestibulum nisl magna, viverra vel ex sit amet, sodales tempor ligula. Aliquam erat volutpat. Aliquam tempor lacinia orci, a iaculis massa luctus ut. Vivamus ut scelerisque sem. Nam vehicula lobortis turpis sed malesuada. Pellentesque vehicula enim ullamcorper neque interdum, at porttitor quam porttitor. Phasellus at quam quis est mollis sodales.
//
//                Phasellus vitae ligula in odio malesuada blandit. Pellentesque a libero nisl. Maecenas sed nibh at elit convallis fermentum venenatis at lectus. Mauris at ante ornare, mollis sapien vitae, molestie diam. Nam non nunc arcu. Sed posuere sapien nisl, vel semper dui bibendum a. Curabitur tortor est, bibendum tincidunt nunc consequat, pharetra sagittis elit. Pellentesque malesuada mi at tellus mollis, et auctor nunc finibus. Proin porttitor finibus odio, ac molestie sapien pharetra at. Etiam gravida imperdiet porttitor. Donec euismod dui nulla, vel bibendum est egestas eget. Curabitur fermentum sollicitudin aliquam.
//
//                Ut porttitor neque facilisis efficitur tempus. Nunc eget interdum ante, vel viverra mauris. Quisque pulvinar mi diam, ac elementum diam commodo in. Vestibulum tincidunt, magna vel convallis varius, ex sem euismod velit, eu convallis lacus felis eu augue. Morbi facilisis magna non arcu sodales, at sodales lacus scelerisque. Fusce et eros tellus. Phasellus venenatis odio non diam vestibulum, sit amet tincidunt mauris maximus. Vestibulum nec ex eget dui rhoncus venenatis a in dui. Quisque pretium, tellus eu ultrices imperdiet, diam ligula eleifend enim, quis fringilla est lectus placerat augue. Praesent facilisis ex eget turpis pellentesque, vel interdum nisl commodo. Nulla consequat consequat justo molestie pulvinar. Donec eget mollis arcu. Pellentesque nec diam id enim volutpat condimentum eget vitae diam. Nullam massa risus, tincidunt sed fringilla id, lacinia id sem. Maecenas dictum urna vel est fermentum bibendum. Suspendisse tincidunt urna a urna faucibus cursus.
//
//                Duis sed accumsan magna, quis feugiat lorem. Donec metus nunc, maximus vel feugiat a, egestas pellentesque arcu. Aliquam condimentum tincidunt nisl nec imperdiet. Aliquam sit amet dui cursus elit laoreet porta ac non nisl. Curabitur dapibus leo odio, et porttitor velit consequat non. Mauris aliquet eu lectus in ullamcorper. Vestibulum vitae leo ipsum. Nullam mollis arcu vel diam posuere dictum nec a nisl. Curabitur ultrices placerat efficitur. Nunc id enim at urna eleifend interdum.
//
//                Integer at urna turpis. Morbi eu tincidunt ligula. Nullam elementum congue enim, vitae porttitor nunc ornare ac. In hac habitasse platea dictumst. Curabitur condimentum vehicula turpis non ornare. Vestibulum ante ipsum primis in faucibus orci luctus et ultrices posuere cubilia curae; Duis vel tellus non nibh gravida tincidunt tempor vitae enim. In et sem efficitur, faucibus nisi eget, egestas mauris. Integer laoreet tincidunt massa vitae fringilla. Sed auctor, dolor id tempus facilisis, ligula turpis iaculis neque, ac vehicula magna ante vel lectus. Donec lacinia augue nisi, id cursus nisl porta ut. Sed id tristique elit.
//
//                Donec mauris felis, maximus eget aliquam venenatis, ornare vel felis. In suscipit mi eu ultricies efficitur. Cras bibendum mauris ex, nec venenatis libero interdum sit amet. Vestibulum quis sapien sapien. Ut lectus magna, posuere in iaculis ut, dapibus nec nisl. Sed fermentum dolor id ipsum feugiat ornare. Quisque feugiat tempus augue quis ultrices.
//
//                Morbi vel eleifend erat, imperdiet placerat diam. Suspendisse tristique a risus sed vehicula. Nam a erat placerat, pulvinar mauris et, rutrum enim. Proin fringilla magna quis neque hendrerit, ac cursus justo pharetra. Etiam ut scelerisque dolor. Sed eget vulputate lectus. Etiam rhoncus finibus sodales.
//
//                Morbi id purus tortor. In mollis finibus mattis. Morbi hendrerit augue odio, at vestibulum justo varius non. Integer pellentesque ut arcu sit amet faucibus. Suspendisse potenti. Maecenas nibh erat, varius bibendum purus vel, tempus accumsan arcu. Donec ut pretium nisi. Ut magna odio, porttitor eget laoreet ac, ornare sodales ipsum.
//
//                Ut condimentum malesuada vulputate. Donec faucibus magna a porta ultricies. Sed posuere maximus velit, nec faucibus urna pharetra non. Ut ultricies nunc id turpis molestie, dapibus varius elit lobortis. Nulla interdum condimentum purus, at efficitur ipsum maximus ut. Duis ac vestibulum sem. Quisque blandit, sapien vitae luctus fermentum, arcu dui dictum sem, quis dictum purus tortor in dui.
//
//                Nullam viverra fermentum nulla eu dapibus. Sed nisl elit, egestas ut pellentesque sit amet, sodales non lectus. Fusce rhoncus nec nisi blandit tincidunt. Cras finibus purus ut metus sagittis, vel tempor risus elementum. Cras ac tortor enim. Aenean nibh ex, scelerisque nec urna ut, egestas ornare metus. Fusce id ligula non libero iaculis dignissim quis et metus. Sed ut sapien eget sem placerat gravida eget ut nulla. Ut vestibulum lorem purus, in varius tellus commodo ac. Suspendisse egestas maximus eros, eget tristique ipsum vehicula vitae. Class aptent taciti sociosqu ad litora torquent per conubia nostra, per inceptos himenaeos. Vivamus ultricies interdum varius. Vestibulum ante ipsum primis in faucibus orci luctus et ultrices posuere cubilia curae;
//
//                Sed faucibus, quam nec sollicitudin volutpat, arcu ex faucibus ipsum, eu lacinia quam sapien id tellus. Maecenas ex nibh, rutrum eu elit a, semper lacinia nulla. Proin tristique metus sem, in tincidunt turpis mollis quis. Proin in ante nisl. Maecenas dignissim, mauris a aliquet ornare, justo nulla lobortis turpis, vel laoreet est mauris ac diam. Maecenas egestas est nec felis pharetra, vel tempus massa facilisis. Donec sodales varius molestie. Ut et ullamcorper nulla, eu elementum felis. Donec vitae cursus nunc, nec euismod arcu. Praesent convallis a dui et viverra. Proin iaculis tortor non leo ullamcorper, et cursus velit blandit. Maecenas ligula dolor, faucibus sit amet neque vitae, luctus sagittis elit. In mauris libero, consectetur sit amet libero quis, ultricies efficitur dolor. Curabitur at metus eu felis scelerisque pulvinar. Nam iaculis ex non eros pulvinar, in mattis neque iaculis.
//
//                Maecenas eu consequat sem. Proin at arcu aliquam, aliquam risus eu, porttitor urna. Curabitur accumsan justo metus, sed feugiat ex laoreet in. Etiam non risus nec massa eleifend rutrum. Nulla id eros id risus sodales tincidunt. Proin lacinia mauris in molestie fermentum. Sed non lacus commodo, tempor justo nec, dapibus nunc. Donec iaculis fringilla nulla, vel blandit velit facilisis sit amet. Nunc vitae ornare leo, ut tincidunt ante.
//
//                Sed sed libero quis mi vestibulum viverra. Donec ullamcorper ultrices placerat. Mauris nunc orci, suscipit vel pellentesque vitae, tincidunt et nulla. Maecenas non metus ligula. Proin sit amet accumsan dui. Sed consequat, quam ac varius dapibus, erat mi viverra leo, ut scelerisque sapien quam volutpat nunc. Ut leo tortor, mollis eget scelerisque nec, rhoncus pellentesque justo. Nam tellus nulla, blandit vel est vel, porta tempus elit. Cras porttitor efficitur nibh, vel varius est tristique a. Aenean malesuada metus sit amet eros tristique tempus. Sed mattis, orci ut facilisis ornare, sem neque fringilla risus, ut molestie risus urna ut justo. Maecenas vestibulum malesuada ipsum. Curabitur tincidunt finibus arcu, ut faucibus nisl vulputate ut. Vestibulum sapien erat, faucibus ut risus sit amet, sagittis malesuada diam. Morbi sed eros non lectus vulputate cursus sit amet a lacus.
//                """;
//
//        byte[] data = loremIpsum.getBytes(StandardCharsets.UTF_8);
//        disk.createFile("/test", data);
//        byte[] data2 = disk.readFile("/test");
//
//        String s = new String(data2, StandardCharsets.UTF_8);
//        assertEquals(loremIpsum, s);
//
//        disk.close();
//
//        VDisk disk2 = VDisk.open(Paths.get("test.vdisk"));
//        byte[] data3 = disk2.readFile("/test");
//
//        String s2 = new String(data3, StandardCharsets.UTF_8);
//        assertEquals(loremIpsum, s2);
//
//        disk2.close();
//    }
}