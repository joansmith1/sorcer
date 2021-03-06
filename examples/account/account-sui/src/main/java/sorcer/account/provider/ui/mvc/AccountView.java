/*
 * Copyright 2014 original autors
 * Copyright 2014 Sorcersoft.com S.A.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package sorcer.account.provider.ui.mvc;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.util.Observable;
import java.util.Observer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import net.jini.core.lookup.ServiceItem;
import sorcer.account.provider.Account;
import sorcer.account.provider.Money;

public class AccountView extends JPanel implements Observer {
	
	private static final long serialVersionUID = -3812646466769297683L;

	private JTextField balanceTextField;

	private JTextField withdrawalTextField;

	private JTextField depositTextField;

	private AccountModel model;

	private AccountDispatcher dispatcher;

	private final static Logger logger = LoggerFactory
			.getLogger("sorcer.provider.account.ui.mvc");

	public AccountView(Object provider) {
		super();
		getAccessibleContext().setAccessibleName("AccountView Tester");
		ServiceItem item = (ServiceItem) provider;

		if (item.service instanceof Account) {
			Account account = (Account) item.service;
			model = new AccountModel();
			dispatcher = new AccountDispatcher(model, this, account);
			createView();
			model.addObserver(this);
			dispatcher.getBalance();
		}
	}

	protected void createView() {
		setLayout(new BorderLayout());
		add(buildAccountPanel(), BorderLayout.CENTER);
	}

	private JPanel buildAccountPanel() {
		JPanel panel = new JPanel();
		JPanel actionPanel = new JPanel(new GridLayout(3, 3));

		actionPanel.add(new JLabel("Current Balance"));
		balanceTextField = new JTextField();
		balanceTextField.setEnabled(false);
		actionPanel.add(balanceTextField);
		actionPanel.add(new JLabel(" cents"));

		actionPanel.add(new JLabel(AccountModel.WITHDRAW));
		withdrawalTextField = new JTextField();
		actionPanel.add(withdrawalTextField);
		JButton withdrawalButton = new JButton("Do it");
		withdrawalButton.setActionCommand(AccountModel.WITHDRAW);
		withdrawalButton.addActionListener(dispatcher);
		actionPanel.add(withdrawalButton);

		actionPanel.add(new JLabel(AccountModel.DEPOSIT));
		depositTextField = new JTextField();
		actionPanel.add(depositTextField);
		JButton depositButton = new JButton("Do it");
		depositButton.setActionCommand(AccountModel.DEPOSIT);
		depositButton.addActionListener(dispatcher);
		actionPanel.add(depositButton);

		panel.add(actionPanel);
		return panel;
	}

	public Money getDepositAmount() {
		return readTextField(depositTextField);
	}

	public Money getWithdrawalAmount() {
		return readTextField(withdrawalTextField);
	}

	public void clearDepositAmount() {
		depositTextField.setText("");
	}

	public void clearWithdrawalAmount() {
		withdrawalTextField.setText("");
	}

	public void displayBalance() {
		Money balance = model.getBalance();
		balanceTextField.setText(balance.value());
	}

	private Money readTextField(JTextField moneyField) {
		try {
			Float floatValue = new Float(moneyField.getText());
			float actualValue = floatValue.floatValue();
			int cents = (int) (actualValue * 100);
			return new Money(cents);
		} catch (Exception e) {
			logger.info("Field doesn't contain a valid value");
		}
		return null;
	}

	public void update(Observable o, Object arg) {
		logger.info("update>>arg: " + arg);
		if (arg != null) {
			if (arg.equals(AccountModel.DEPOSIT))
				clearDepositAmount();
			else if (arg.equals(AccountModel.WITHDRAW))
				clearWithdrawalAmount();
			else if (arg.equals(AccountModel.BALANCE))
				displayBalance();
		}
	}
}
