package sample;

import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class Controller{
    public static final int TEXTAREA_WIDTH = 416;
    public static final int TEXTAREA_HEIGHT = 446;

    private final DirectoryChooser directoryChooser = new DirectoryChooser();
    public Stage primaryStage;

    private String nameRootDir = "";
    private TreeItem<String> rootTreeView = new TreeItem<>();

    @FXML private TextField textFieldFolderPath;
    @FXML private TextField textFieldSearchText;
    @FXML private TextField textFieldFileExtension;
    @FXML private TreeView<String> treeViewFolders;
    @FXML private TabPane tabPane;
    @FXML private Label labelSearchInProgress;

    @FXML
    private void initialize () {
        String initialValueFolderPath = System.getProperty("user.home");
        textFieldFolderPath.setText(initialValueFolderPath);
        nameRootDir = new File(initialValueFolderPath).getName();

        labelSearchInProgress.setVisible(false);
    }

    @FXML
    private void clickButtonChooseFolder(ActionEvent event) {
        configuringDirectoryChooser(directoryChooser);

        File dir = directoryChooser.showDialog(primaryStage);

        if (dir == null) {
            textFieldFolderPath.setText(null);
        } else {
            textFieldFolderPath.setText(dir.getAbsolutePath());
            nameRootDir = dir.getName();
        }
    }

    @FXML
    private void clickButtonSearch(ActionEvent event) {
        search();
    }

    @FXML
    private void onKeyPressed(KeyEvent keyEvent) {
        if (keyEvent.getCode() == KeyCode.ENTER){
            search();
        }
    }

    @FXML
    private void clickTreeView(MouseEvent event) {
        MultipleSelectionModel<TreeItem<String>> selectedItem = treeViewFolders.getSelectionModel();

        if (!selectedItem.isEmpty()) {
            if (selectedItem.getSelectedItem().isLeaf()) {
                String selectedItemValue = selectedItem.getSelectedItem().getValue();
                if (selectedItemValue != null) {
                    TreeItem<String> parentName = selectedItem.getSelectedItem().getParent();
                    TreeItem<String> nameParentIfExistOrNull = parentName.getParent();
                    String path = "";

                    while (nameParentIfExistOrNull != null) {
                        path = parentName.getValue() + "/";

                        if (!nameParentIfExistOrNull.getValue().equals(nameRootDir)) {
                            path = nameParentIfExistOrNull.getValue() + "/" + path;
                        }

                        nameParentIfExistOrNull = nameParentIfExistOrNull.getParent();
                    }

                    findOrCreateTab(selectedItem, path);
                }
            }
        }
    }

    private void findOrCreateTab(MultipleSelectionModel<TreeItem<String>> selectedItem, String path) {
        String strNameChooseFile = selectedItem.getSelectedItem().getValue();
        List<String> linesOfFile;
        try {
            linesOfFile = Files.readAllLines(Paths.get(textFieldFolderPath.getText() + "/" + path + strNameChooseFile));
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Error!", "Error in file extension!");
            return;
        }

        ObservableList<Tab> tabs = tabPane.getTabs();
        for (Tab tab : tabs){
            if (tab.getText().equals(strNameChooseFile)){
                tabPane.getSelectionModel().select(tab);
                return;
            }
        }

        createNewTab(strNameChooseFile, linesOfFile);
    }

    private void createNewTab(String strNameChooseFile, List<String> linesOfFile) {
        TextArea textArea1 = new TextArea();
        textArea1.setMinWidth(TEXTAREA_WIDTH);
        textArea1.setMaxWidth(TEXTAREA_WIDTH);
        textArea1.setMinHeight(TEXTAREA_HEIGHT);
        textArea1.setMaxHeight(TEXTAREA_HEIGHT);

        Tab tab = new Tab(strNameChooseFile, textArea1);
        tabPane.getTabs().add(tab);
        tabPane.getSelectionModel().select(tab);

        for (String line : linesOfFile) {
            textArea1.appendText(line + "\n");
        }
    }

    private void search() {
        labelSearchInProgress.setVisible(true);
        treeViewFolders.setRoot(null);
        tabPane.getTabs().clear();

        fillTreeView();

        if (rootTreeView.getChildren().isEmpty()) {
            showAlert("Empty!", "Empty\nThe specified text was not found in the files.");
        } else {
            treeViewFolders.setRoot(rootTreeView);
            treeViewFolders.getRoot().setValue(nameRootDir);
            treeViewFolders.getRoot().setExpanded(true);
        }

        labelSearchInProgress.setVisible(false);
    }

    private void fillTreeView(){
        String strFolderPath = textFieldFolderPath.getText();

        if (strFolderPath == null) {
            showAlert("Error!", "Error in folder path!");
            return;
        }

        try {
            rootTreeView.getChildren().clear();

            searchInFolders(rootTreeView, strFolderPath);
        }
        catch (Exception e) {
            e.printStackTrace();
            System.out.println("Error: " + e.getMessage());
        }
    }

    private int searchInFolders(TreeItem<String> current, String folderPath) {
        String strFileExtension = textFieldFileExtension.getText();
        File folder = new File(folderPath);
        File[] folderEntries = folder.listFiles();

        int i = 0;

        if (folderEntries != null) {
            for (File entry : folderEntries)
            {
                if (entry.isDirectory())
                {
                    TreeItem<String> subFolder = new TreeItem<>(entry.getName());
                    int result = searchInFolders(subFolder, folderPath + "/" + entry.getName());

                    if (result > 0) {
                        subFolder.setExpanded(true);
                        rootTreeView.getChildren().add(subFolder);
                    }
                    else {
                        return -1;
                    }
                }
                else {
                    String fileName = entry.getName();

                    if (strFileExtension.equals("") || !strFileExtension.substring(0, 1).equals(".")) {
                        showAlert("Error!", "Error in File Extension!");
                        return -1;
                    }

                    if (fileName.endsWith(strFileExtension)){
                        String strFind = findTargetInFile(fileName, folderPath);

                        if (strFind.equals("Error")){
                            return -1;
                        }

                        if (strFind.equals("OK")) {
                            current.getChildren().add(new TreeItem<>(fileName));
                            i++;
                        }
                    }
                }
            }
        }
        return i;
    }

    private String findTargetInFile(String file, String strFolderPath) {
        String target = textFieldSearchText.getText();

        List<String> lines;
        try {
            lines = Files.readAllLines(Paths.get(strFolderPath + "/" + file));
        } catch (IOException e) {
            showAlert("Error!", "Error in file extension!");
            e.printStackTrace();
            return "Error";
        }

        for (String line : lines) {
            if (line.contains(target)) {
                return "OK";
            }
        }

        return "NO";
    }

    private void configuringDirectoryChooser(DirectoryChooser directoryChooser) {
        directoryChooser.setTitle("Select Some Directories");
        directoryChooser.setInitialDirectory(new File(System.getProperty("user.home")));
    }

    private void showAlert(String title, String message){
        Alert alert = new Alert(Alert.AlertType.INFORMATION);

        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);

        alert.showAndWait();
    }
}